package com.tau.ephuapp.activities.main.ui.certificate.scan

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKManager.FEATURE_TYPE
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.BarcodeScannerActivity
import com.tau.ephuapp.activities.CertificateActivity
import com.tau.ephuapp.activities.CertificateActivityViewModel
import com.tau.ephuapp.adapters.CertificationTaskItemAdapter
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentScanBinding
import com.tau.ephuapp.models.Certification
import com.tau.ephuapp.models.CertificationTaskItem
import com.tau.ephuapp.models.Item
import com.tau.ephuapp.services.MyWorkerManagerService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class ScanFragment : Fragment(), EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    // Variables to hold EMDK related objects
    private var emdkManager: EMDKManager? = null
    private var scanSuccessBeep: MediaPlayer? = null
    private var scanFailBeep: MediaPlayer? = null
    private var scanErrorBeep: MediaPlayer? = null
    private var scanExistsBeep: MediaPlayer? = null
    private var scanTriggerBeep: MediaPlayer? = null
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null
    // Variables to hold handlers of UI controls
    private val viewModel: CertificateActivityViewModel by activityViewModels()
    private var certificatedItemsShort: MutableList<CertificationTaskItem> = mutableListOf()
    private var mAdapter: CertificationTaskItemAdapter? = null
    private var db: AppDatabase? = null
    // view binding
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding
    private enum class FoundBy {
        FOUND_BY_EAN13, FOUND_BY_EAN14, FOUND_BY_SKU, NOT_FOUND
    }
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        Log.i(TAG, "on create view...")
        db = AppDatabase.getDatabase(requireContext())
        Log.i(TAG, "Restoring state...")
        scanSuccessBeep = MediaPlayer.create(context, R.raw.success)
        scanFailBeep = MediaPlayer.create(context, R.raw.fail)
        scanTriggerBeep = MediaPlayer.create(context, R.raw.trigger)
        scanExistsBeep = MediaPlayer.create(context, R.raw.exists)
        scanErrorBeep = MediaPlayer.create(context, R.raw.error)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "on view created...")
        (requireActivity() as CertificateActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as CertificateActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        binding?.overlayTv?.visibility = View.GONE
        mAdapter = CertificationTaskItemAdapter(certificatedItemsShort, this.requireContext())
        binding?.reciclerView?.layoutManager = LinearLayoutManager(this.requireContext())
        binding?.reciclerView?.adapter = mAdapter
        updateCounters()
        initEMDK()
        val triggerSearch = {
            if (binding?.barcodeEt?.text?.isNotEmpty() == true) {
                doAsync {
                    searchData(binding?.barcodeEt?.text.toString())
                }
            }
        }
        binding?.barcodeEt?.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    triggerSearch()
                    true
                } else -> false
            }
        }
        binding?.barcodeEt?.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                triggerSearch()
            }
            false
        })
        viewModel.certifiedItems.observe(viewLifecycleOwner, Observer { certifiedItems ->
            Log.i(TAG, "actualizando certifiedLinesShort con ${certifiedItems}")
            doAsync {
                certificatedItemsShort.clear()
                if (!certifiedItems.isNullOrEmpty()) {
                    certifiedItems.get(0).taskId.let { taskId ->
                        db?.certificationsDao()?.getAllByTaskGroupedByItemId(taskId)
                                ?.let { certifiedItemsGroupedByItemId ->
                                    Log.i(TAG, "certified items grouped by item: $certifiedItemsGroupedByItemId")
                                    if (certifiedItemsGroupedByItemId.size > 5) {
                                        certificatedItemsShort.addAll(certifiedItemsGroupedByItemId.subList(0, 5))
                                    } else if (!certifiedItemsGroupedByItemId.isNullOrEmpty()) {
                                        certificatedItemsShort.addAll(certifiedItemsGroupedByItemId)
                                    } else {

                                    }
                                }
                    }
                }
                uiThread {
                    mAdapter?.notifyDataSetChanged()
                    updateCounters()
                }
            }
        })
        binding?.barcodeEt?.setOnTouchListener(View.OnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding?.barcodeEt?.right
                                ?: 0) - (binding?.barcodeEt?.getCompoundDrawables()
                                ?.get(DRAWABLE_RIGHT)?.bounds?.width() ?: 0)
                ) {
                    triggerSearch()
                    return@OnTouchListener true
                } else if (event.rawX <= (binding?.barcodeEt?.left
                                ?: 0) + (binding?.barcodeEt?.getCompoundDrawables()
                                ?.get(DRAWABLE_LEFT)?.bounds?.width() ?: 0)
                ) {
                    startActivityForResult(Intent(requireContext(),
                            BarcodeScannerActivity::class.java), BARCODE_SCANNER
                    )
                    return@OnTouchListener true
                }
            }
            false
        })
    }

    fun initEMDK() {
        try {
            // Requests the EMDKManager object. This is an asynchronous call and should be called from the main thread.
            // The callback also will receive in the main thread without blocking it until the EMDK resources are ready.
            val results = EMDKManager.getEMDKManager(this.requireContext(), this)
            // Check the return status of getEMDKManager() and update the status TextView accordingly.
            if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
                updateStatus("Solicitud de lectura fallida!")
            } else {
                updateStatus("Inicializacion en progreso...")
            }
        }catch (e: Exception) {
            //updateStatus("Error loading EMDK Manager")
            Log.e(TAG, "Error loading EMDK Manager")
        }
    }

    private fun showSnackbar(message: String) {
        requireActivity().runOnUiThread {
            requireView().let {
                Snackbar.make(it,
                        message,
                        Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun updateCounters() {
        requireActivity().runOnUiThread {
            val totalCertified = (viewModel.certifiedItems.value?.sumBy {
                it.quantity
            } ?: 0)
            val totalToCertificate = (viewModel.currentCertificationTaskItems.value?.sumBy {
                it.totalUnits
            } ?: 0) - totalCertified
                binding?.pendingTv?.text = totalToCertificate.toString()
                binding?.certifiedTv?.text = totalCertified.toString()
        }
    }

    private fun initBarcodeManager() {
        // Get the feature object such as BarcodeManager object for accessing the feature.
        barcodeManager = emdkManager?.getInstance(FEATURE_TYPE.BARCODE) as BarcodeManager
        // Add external scanner connection listener.
        if (barcodeManager == null) {
            showSnackbar("Barcode scanning is not supported")
        }
    }

    private fun initScanner() {
        if (scanner == null) {
            // Get default scanner defined on the device
            scanner = barcodeManager?.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            if (scanner != null) {
                // Implement the DataListener interface and pass the pointer of this object to get the data callbacks.
                scanner?.addDataListener(this)
                // Implement the StatusListener interface and pass the pointer of this object to get the status callbacks.
                scanner?.addStatusListener(this)
                // Hard trigger. When this mode is set, the user has to manually
                // press the trigger on the device after issuing the read call.
                // NOTE: For devices without a hard trigger, use TriggerType.SOFT_ALWAYS.
                scanner?.triggerType = Scanner.TriggerType.HARD
                try {
                    // Enable the scanner
                    // NOTE: After calling enable(), wait for IDLE status before calling other scanner APIs
                    // such as setConfig() or read().
                    scanner?.enable()
                } catch (e: ScannerException) {
                    Log.e(TAG, "Ocurrio una Excepcion ${e.message}", e)
                    updateStatus("Ocurrio un error con el lector")
                    deInitScanner()
                }
            } else {
                updateStatus("La inicializacion del lector ha fallado")
            }
        }
    }

    private fun deInitScanner() {
        if (scanner != null) {
            try {
                // Release the scanner
                scanner?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Ocurrio un error al inicializar el escaner")
                updateStatus(e.message)
            }
            scanner = null
        }
    }

    override fun onOpened(_emdkManager: EMDKManager?) {
        // Get a reference to EMDKManager
        emdkManager =  _emdkManager
        // Get a  reference to the BarcodeManager feature object
        initBarcodeManager()
        // Initialize the scanner
        initScanner()
    }

    override fun onClosed() {
        // The EMDK closed unexpectedly. Release all the resources.
        emdkManager?.release();
        emdkManager = null;
        updateStatus("Se cerro el escaner inesperadamente. Por favor reinicie la app");
    }

    override fun onStatus(statusData: StatusData?): Unit {
        // The status will be returned on multiple cases. Check the state and take the action.
        // Get the current state of scanner in background
        val state: StatusData.ScannerStates = statusData?.state ?: StatusData.ScannerStates.IDLE
        var statusStr: String = ""
        // Different states of Scanner
        when (state) {
            StatusData.ScannerStates.IDLE -> {
                // Scanner is idle and ready to change configuration and submit read.
                statusStr = "Escaner activado y listo para leer..."
                // Change scanner configuration. This should be done while the scanner is in IDLE state.
                setConfig()
                try {
                    // Starts an asynchronous Scan. The method will NOT turn ON the scanner beam,
                    //but puts it in a  state in which the scanner can be turned on automatically or by pressing a hardware trigger.
                    scanner?.read()
                } catch (e: ScannerException) {
                    Log.e(TAG, "Ocurrio una excepcion con el escaner", e)
                    updateStatus("Ocurrio un error inesperado con el escaner")
                }
            }
            StatusData.ScannerStates.WAITING -> {
                // Scanner is waiting for trigger press to scan...
                statusStr = "Escaner esperando por datos..."
            }
            StatusData.ScannerStates.SCANNING -> {
                // Scanning is in progress...
                statusStr = "Escaneando..."
            }
            StatusData.ScannerStates.DISABLED -> {
                // Scanner is disabled
                statusStr = "El escaner esta desactivado"
            }
            StatusData.ScannerStates.ERROR -> {
                // Error has occurred during scanning
                statusStr = "Ha ocurrido un error"
            }
        }
        // Updates TextView with scanner state on UI thread.
        updateStatus(statusStr);
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        // The ScanDataCollection object gives scanning result and the collection of ScanData. Check the data and its status.
        var barcodeData: String = ""
        if ((scanDataCollection != null) && (scanDataCollection.result == ScannerResults.SUCCESS)) {
            val scanData: ArrayList<ScanDataCollection.ScanData> = scanDataCollection.scanData;
            // Iterate through scanned data and prepare the data.
            for (data: ScanDataCollection.ScanData in scanData) {
                // Get the scanned dataString
                barcodeData = data.data
                Log.i(TAG, "tipo de codigo escaneado: ${data.labelType}")
                Log.i("DATA_LOADED", barcodeData)
            }
            // limpiamos input
            requireActivity().runOnUiThread {
                binding?.barcodeEt?.text?.clear()
                binding?.barcodeEt?.setText(barcodeData)
            }
            // Updates EditText with scanned data
            doAsync {
                searchData(barcodeData)
            }
        }
    }

    fun searchData(barcode: String) {
        Log.i(TAG, "barcode readed: $barcode")
        val foundItemBySku = db?.itemDao()?.getBySku(barcode)
        val foundItemByEan13 = db?.itemDao()?.getByEan13(barcode)
        val foundItemByEan14 = db?.itemDao()?.getByEan14(barcode)
        var foundBy: FoundBy = FoundBy.NOT_FOUND
        var foundItem: Item? = null
        var existInTask = false
        var quantity: Int = 0
        var howManyCertified: Int = 0
        val doCertification = { _item: Item, _quantity: Int ->
            Log.i(TAG, "doing certification qty: $_quantity and item:$_item")
            val certificationItem = viewModel.currentCertificationTaskItems.value?.find { d ->
                d.itemId == _item.id && d.taskQuantity < d.totalUnits && (quantity + howManyCertified) <= d.totalUnits
            }
            if(certificationItem != null){
                Log.i(TAG, "itemID:${_item.id} found in pending list")
                scanSuccessBeep?.start()
                updateStatus(getString(R.string.item_found))
                Log.i(TAG, "econtrado: $certificationItem")
                addCertification(certificationItem, _quantity)
                requireActivity().runOnUiThread {
                    binding?.barcodeEt?.setText("")
                }
            } else {
                scanErrorBeep?.start()
                updateStatus(getString(R.string.item_not_found_in_task))
            }
        }
        if(foundItemByEan13 != null){
            quantity = 1
            foundItem = foundItemByEan13
            foundBy = FoundBy.FOUND_BY_EAN13
        } else if (foundItemByEan14 != null) {
            quantity = foundItemByEan14.packaging ?: 0
            foundItem = foundItemByEan14
            foundBy = FoundBy.FOUND_BY_EAN14
        } else if(foundItemBySku != null){
            quantity = 1
            foundItem = foundItemBySku
            foundBy = FoundBy.FOUND_BY_SKU
        }
        if (foundItem == null) {
            scanFailBeep?.start()
            updateStatus(getString(R.string.sku_not_found_in_db))
            return
        }
        howManyCertified = viewModel.certifiedItems.value?.sumBy {
            if(it.itemId == foundItem?.id && it.taskId == viewModel.task.value?.id) {
                it.quantity
            } else {
                0
            }
        } ?: 0
        Log.i(TAG, "items certificados hasta ahora: $howManyCertified")
        viewModel.currentCertificationTaskItems.value?.forEach {
            if(it.itemId == foundItem.id) {
                existInTask = true
                if ((howManyCertified + quantity) <= it.totalUnits){
                    doCertification(foundItem, quantity)
                } else {
                    // already certified
                    scanExistsBeep?.start()
                    updateStatus(getString(R.string.item_already_certified))
                }
                return@forEach
            }
        }
        if (!existInTask) {
            Log.i(TAG, "el item no existe en la tarea")
            // doesn't exist in task
            scanFailBeep?.start()
            updateStatus(getString(R.string.item_not_found_in_task))
        }
    }

    private fun addCertification(foundItem: CertificationTaskItem, quantity: Int) {
        //viewModel.pendingCertificationTaskItems.value?.remove(foundItem)
        val certification = Certification(itemId = foundItem.itemId, taskId = foundItem.taskId, quantity = quantity)
        //viewModel.certifiedItems.value?.add(0, certification)
        db?.certificationsDao()?.insert(certification)
        //viewModel.repository.setCertifiedItems(viewModel.certifiedItems.value)
        MyWorkerManagerService.enqueUploadSingleCertificationWork(requireContext(), certification)
    }

    fun showAlert(title: String, message: String, listener: (() -> Unit?)? = null) {
        requireActivity().runOnUiThread {
            val builder = AlertDialog.Builder(this.requireActivity())
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(getString(R.string.accept), DialogInterface.OnClickListener { _, _ ->
                listener?.invoke()
            })
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }
    private fun setConfig() {
        try {
            // Get scanner config
            val config = scanner?.config
            // Enable haptic feedback
            if (config?.isParamSupported("config.scanParams.decodeHapticFeedback")!!) {
                config.scanParams.decodeHapticFeedback = true
            }
            // Set scanner config
            scanner?.config = config
        } catch (e: ScannerException) {
            Log.e(TAG, "Error al configurar escaner", e)
            updateStatus("Ocurrio un error al configurar escaner")
        }
    }

    private fun updateStatus(status: String?) {
        Log.i(TAG, status ?: "")
        requireActivity().runOnUiThread{
            // Update the status text view on UI thread with current scanner state
            binding?.scannerStatusTv?.text = status
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == BARCODE_SCANNER){
            val barcode: String = data?.getStringExtra("barcode") ?: ""
            Log.i(TAG, "datos del escaneo: ${data?.extras}")
            binding?.barcodeEt?.text?.clear()
            barcode.let{
                binding?.barcodeEt?.setText(it)
                doAsync {
                    searchData(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emdkManager?.release(FEATURE_TYPE.BARCODE)
        emdkManager = null
        scanTriggerBeep?.release()
        scanTriggerBeep = null
        scanFailBeep?.release()
        scanFailBeep = null
        scanSuccessBeep?.release()
        scanSuccessBeep = null
    }

    companion object{
        private const val BARCODE_SCANNER = 0
        private const val TAG = "SCAN_FRAGMENT"
    }
}