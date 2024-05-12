package com.example.newproject.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.clusterproject.IBikeSpeedCallback
import com.example.clusterproject.IIPCExample
import com.example.newproject.MainActivity
import com.example.newproject.base.BaseFragment
import com.example.newproject.databinding.FragmentIpcBinding

class IpcFragment : BaseFragment<MainActivity>(), ServiceConnection {

    private lateinit var binding: FragmentIpcBinding
    private var connected = false
    private var iRemoteService: IIPCExample? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentIpcBinding.inflate(inflater, container, false)
        registerListeners()
        return binding.root
    }

    override fun registerListeners() {
        super.registerListeners()

        binding.btnConnect.setOnClickListener {
            connected = if (connected) {
                disconnectToRemoteService()
                binding.apply {
                    tvPID.text = ""
                    tvConnectionStatus.text = ""
                    btnConnect.text = "Connect"
                }
                false
            } else {
                connectToRemoteService()
                binding.btnConnect.text = "Disconnect"
                true
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d(TAG, "onServiceConnected: $name")
        Toast.makeText(requireContext(), "Service Connected", Toast.LENGTH_SHORT).show()
        // Gets an instance of the AIDL interface named IIPCExample,
        // which we can use to call on the service
        iRemoteService = IIPCExample.Stub.asInterface(service)  //or service as IIPCExample.Stub

        binding.tvPID.text = "PID:${iRemoteService?.pid}"
        binding.tvConnectionStatus.text = "Connection Count:${iRemoteService?.connectionCount}"

        //send the data to remote service
        iRemoteService?.setDisplayedValue(
            context?.packageName,
            Process.myPid(),//Returns the identifier of this process, which can be used with killProcess and sendSignal.
            binding.editClientData.text.toString()
        )

        // Register the callback with the service
        iRemoteService?.registerConnectionCountCallback(callback)
        connected = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected: ")
        Toast.makeText(
            requireContext(),
            "IPC server has disconnected unexpectedly",
            Toast.LENGTH_LONG
        )
            .show()
        iRemoteService = null
        connected = false
    }

    //onBikeSpeedChanged this is called whenever bike speed change in server
    val callback = object : IBikeSpeedCallback.Stub() {
        override fun onBikeSpeedChanged(count: Int) {
            // Handle connection count change
            Log.d(TAG, "onConnectionCountChanged Client : $count")
            binding.tvPID.text = count.toString()
        }
    }

    private fun connectToRemoteService() {
        val intent =
            Intent(INTENT_ACTION_AIDL) //pass the action which is used to communication through aidl or messenger
        val pack = IIPCExample::class.java.`package`
        pack?.let {
            intent.setPackage(pack.name)
            //bind the remote service
            activity?.bindService(
                intent, this, Context.BIND_AUTO_CREATE
            )
        }
//        val intent = Intent()
//        intent.component=ComponentName("com.example.newproject","com.example.clusterproject.AIDLService")
//        activity?.applicationContext?.bindService(
//                intent, this, Context.BIND_AUTO_CREATE
//            )
    }

    //unbind the remote service
    private fun disconnectToRemoteService() {
        if (connected) {
            iRemoteService?.unregisterConnectionCountCallback(callback)
            activity?.unbindService(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister the callback from the service (when no longer needed)
        iRemoteService?.unregisterConnectionCountCallback(callback)
    }

    companion object {
        private const val TAG = "IpcFragment"
        private const val INTENT_ACTION_AIDL = "aidlexample"
    }
}