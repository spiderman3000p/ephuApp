package com.tau.ephuapp.activities.main.ui.certificate.pending

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.barcode.*
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.CertificateActivity
import com.tau.ephuapp.activities.CertificateActivityViewModel
import com.tau.ephuapp.adapters.CertificationTaskItemAdapter
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentListBinding
import com.tau.ephuapp.models.CertificationTaskItem
import org.jetbrains.anko.doAsync

class PendingFragment : Fragment(), EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    // Variables to hold EMDK related objects
    private var emdkManager: EMDKManager? = null
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null
    private val viewModel: CertificateActivityViewModel by activityViewModels()
    private var db: AppDatabase? = null
    private val filteredData: MutableList<CertificationTaskItem> = mutableListOf()
    private var mAdapter: CertificationTaskItemAdapter? = null
    var _binding: FragmentListBinding? = null
    val binding get() = _binding
    private var scanFailBeep: MediaPlayer? = null
    private var scanSuccessBeep: MediaPlayer? = null
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "on create view")
        _binding = FragmentListBinding.inflate(inflater, container, false)
        db = AppDatabase.getDatabase(requireContext())
        scanFailBeep = MediaPlayer.create(context, R.raw.fail)
        scanSuccessBeep = MediaPlayer.create(context, R.raw.success)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as CertificateActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as CertificateActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        Log.i(TAG, "on view created")
        mAdapter = CertificationTaskItemAdapter(filteredData, this.requireContext(), false)
        binding?.recyclerView?.layoutManager = LinearLayoutManager(this.requireContext())
        binding?.recyclerView?.adapter = mAdapter
        binding?.searchEt?.visibility = View.VISIBLE
        binding?.searchEt?.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                if (binding?.searchEt?.text?.isNotEmpty() == true) {
                    searchData(binding?.searchEt?.text.toString())
                } else {
                    filteredData.clear()
                    filteredData.addAll(viewModel.pendingCertificationTaskItems.value!!)
                    mAdapter?.notifyDataSetChanged()
                }
            }
            false
        }
        binding?.searchEt?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                if(v.text.isNotEmpty()){
                    searchData(v.text.toString())
                } else {
                    filteredData.clear()
                    filteredData.addAll(viewModel.pendingCertificationTaskItems.value!!)
                    mAdapter?.notifyDataSetChanged()
                }
            }
            true
        }
        binding?.searchEt?.setOnTouchListener(View.OnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding?.searchEt?.right
                                ?: 0) - (binding?.searchEt?.getCompoundDrawables()
                                ?.get(DRAWABLE_RIGHT)?.bounds?.width() ?: 0)
                ) {
                    searchData(binding?.searchEt?.text.toString())
                    return@OnTouchListener true
                }/* else if (event.rawX <= (binding?.searchEt?.left
                                ?: 0) + (binding?.searchEt?.getCompoundDrawables()
                                ?.get(DRAWABLE_LEFT)?.bounds?.width() ?: 0)
                ) {
                    startActivityForResult(Intent(requireContext(),
                            BarcodeScannerActivity::class.java), ScanFragment.BARCODE_SCANNER
                    )
                    return@OnTouchListener true
                }*/
            }
            false
        })
        viewModel.pendingCertificationTaskItems.observe(viewLifecycleOwner, Observer{ pendingCertifications ->
            Log.i(TAG, "pending certifications items observed: $pendingCertifications")
            filteredData.clear()
            if(!pendingCertifications.isNullOrEmpty()) {
                filteredData.addAll(pendingCertifications.toMutableList())
            }
            mAdapter?.notifyDataSetChanged()
        })
        initEMDK()
    }

    fun initEMDK() {
        try {
            // Requests the EMDKManager object. This is an asynchronous call and should be called from the main thread.
            // The callback also will receive in the main thread without blocking it until the EMDK resources are ready.
            val results = EMDKManager.getEMDKManager(this.requireContext(), this)
        }catch (e: Exception) {
            Log.e(TAG, "Error loading EMDK Manager", e)
        }
    }

    private fun initBarcodeManager() {
        // Get the feature object such as BarcodeManager object for accessing the feature.
        barcodeManager = emdkManager?.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
        // Add external scanner connection listener.
        if (barcodeManager == null) {
            Utilities.showToast(requireContext(),"Barcode scanning is not supported")
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
                    //updateStatus(e.message)
                    deInitScanner()
                }
            } else {
                updateStatus("Failed to initialize the scanner device.")
            }
        }
    }

    private fun deInitScanner() {
        if (scanner != null) {
            try {
                // Release the scanner
                scanner?.release()
            } catch (e: Exception) {
                updateStatus(e.message)
            }
            scanner = null
        }
    }

    override fun onOpened(_emdkManager: EMDKManager?) {
        // Get a reference to EMDKManager
        emdkManager =  _emdkManager;
        // Get a  reference to the BarcodeManager feature object
        initBarcodeManager();
        // Initialize the scanner
        initScanner();
    }

    override fun onClosed() {
        // The EMDK closed unexpectedly. Release all the resources.
        emdkManager?.release();
        emdkManager = null;
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.");
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
                statusStr = statusData?.friendlyName + " is   enabled and idle..."
                // Change scanner configuration. This should be done while the scanner is in IDLE state.
                setConfig()
                try {
                    // Starts an asynchronous Scan. The method will NOT turn ON the scanner beam,
                    //but puts it in a  state in which the scanner can be turned on automatically or by pressing a hardware trigger.
                    scanner?.read()
                } catch (e: ScannerException) {
                    updateStatus(e.message);
                }
            }
            StatusData.ScannerStates.WAITING -> {
                // Scanner is waiting for trigger press to scan...
                statusStr = "Scanner is waiting for trigger press..."
            }
            StatusData.ScannerStates.SCANNING -> {
                // Scanning is in progress...
                statusStr = "Scanning..."
            }
            StatusData.ScannerStates.DISABLED -> {
                // Scanner is disabled
                statusStr = statusData?.friendlyName + " is disabled."
            }
            StatusData.ScannerStates.ERROR -> {
                // Error has occurred during scanning
                statusStr = "An error has occurred."
            }
        }
        // Updates TextView with scanner state on UI thread.
        updateStatus(statusStr);
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        // The ScanDataCollection object gives scanning result and the collection of ScanData. Check the data and its status.
        var dataStr: String = ""
        var barcodeData: String
        if ((scanDataCollection != null) && (scanDataCollection.result == ScannerResults.SUCCESS)) {
            val scanData: ArrayList<ScanDataCollection.ScanData> =  scanDataCollection.scanData;
            // Iterate through scanned data and prepare the data.
            for (data: ScanDataCollection.ScanData in  scanData) {
                // Get the scanned dataString
                barcodeData =  data.data;
                Log.i("DATA_LOADED", barcodeData);
                // Get the type of label being scanned
                val labelType: ScanDataCollection.LabelType = data.labelType;
                // Concatenate barcode data and label type
                dataStr = "$barcodeData  $labelType";
            }
            // Updates EditText with scanned data and type of label on UI thread.
            searchData(dataStr)
        }
    }

    fun searchData(barcode: String) {
        Log.i(TAG, "barcode readed: $barcode")
        var foundDeliveryLines: List<CertificationTaskItem>?
        foundDeliveryLines = viewModel.pendingCertificationTaskItems.value?.filter { d ->
            d.itemSku == barcode
        }?.also{
            filteredData.clear()
            filteredData.addAll(it)
            activity?.runOnUiThread {
                mAdapter?.notifyDataSetChanged()
            }
            doAsync {
                scanSuccessBeep?.start()
            }
            updateStatus(getString(R.string.sku_found))
            Log.i(TAG, "econtrado: $it")
        }
        if (foundDeliveryLines.isNullOrEmpty()) {
            doAsync {
                scanFailBeep?.start()
            }
            updateStatus(getString(R.string.sku_not_found))
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
            updateStatus(e.message)
        }
    }

    private fun updateStatus(status: String?) {
        Log.i(TAG, status ?: "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.emdkManager?.release(EMDKManager.FEATURE_TYPE.BARCODE);
        this.emdkManager = null;
    }

    companion object{
        const val TAG = "PENDING_FRAGMENT"
        const val BARCODE_SCANNER = 0
    }
}