package com.tau.ephuapp.activities

//import com.budiyev.android.codescanner.*
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
//import com.tau.ephuapp.databinding.FragmentBarcodeScannerBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
@SuppressLint("LongLogTag")
class BarcodeScannerActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {
    //lateinit var binding: FragmentBarcodeScannerBinding
    //private lateinit var codeScanner: CodeScanner
    private lateinit var mScannerView: ZXingScannerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = FragmentBarcodeScannerBinding.inflate(layoutInflater)
        //setContentView(binding.root)
        mScannerView = ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
        /*val scannerView = binding.scannerView
        codeScanner = CodeScanner(this, scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = listOf(BarcodeFormat.CODE_128, BarcodeFormat.EAN_13, BarcodeFormat.CODE_39) // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not
        codeScanner.decodeCallback = DecodeCallback {
            Log.i(TAG, "barcode escaneado: ${it.text}")
            setResult(RESULT_OK, Intent().putExtra("barcode", it.text))
            finish()
        }
        codeScanner.errorCallback = ErrorCallback {
            Log.e(TAG, "error escaneando: ${it.message}")
        }
        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }*/
    }

    override fun onResume() {
        super.onResume()
        //codeScanner.startPreview()
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    override fun onPause() {
        //codeScanner.releaseResources()
        super.onPause()
        mScannerView.stopCamera();
    }

    override fun onDestroy() {
        //codeScanner.releaseResources()
        mScannerView.stopCamera();
        super.onDestroy()
    }

    override fun handleResult(rawResult: Result?) {
        // Do something with the result here
        val barcode = rawResult?.getText()
        Log.i(TAG, "resultados del scanner: ${barcode}"); // Prints scan results
        Log.i(TAG, " tipo de codigo: ${rawResult?.getBarcodeFormat()}"); // Prints the scan format (qrcode, pdf417 etc.)
        // If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
        mScannerView.stopCamera();
        setResult(RESULT_OK, Intent().putExtra("barcode", barcode))
        finish()
    }

    companion object{
        private const val TAG = "BARCODE_SCANNER_ACTIVITY"
    }
}