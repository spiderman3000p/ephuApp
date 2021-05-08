package com.tau.ephuapp.activities.main.ui.cert_tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.tau.ephuapp.R

class CertificationTasksFragment : Fragment() {

    private lateinit var certificationTasksViewModel: CertificationTasksViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        certificationTasksViewModel =
                ViewModelProvider(this).get(CertificationTasksViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_cert_tasks, container, false)
        val textView: TextView = root.findViewById(R.id.text_gallery)
        certificationTasksViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }
}