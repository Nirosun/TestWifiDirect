/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmu18842.team3.testwifidirect;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cmu18842.team3.testwifidirect.DeviceListFragment.DeviceActionListener;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    //protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;

    private InetAddress destAddr;

    ProgressDialog progressDialog = null;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                        destAddr = null;
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);*/

                        if (destAddr == null) {
                            return;
                        }

                        // Allow user to send message
                        EditText editMessage = (EditText) mContentView.findViewById(R.id.edit_message);
                        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
                        String text = editMessage.getText().toString();
                        statusText.setText("Sending: " + text);
                        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + text);

                        Intent serviceIntent = new Intent(getActivity(), MessageTransferService.class);

                        Message message = new Message();
                        message.setMessageContent(text);

                        //statusText.setText("Message: " + message.getMessageContent());

                        // TODO: Send to anyone
                        serviceIntent.setAction(MessageTransferService.ACTION_SEND_MESSAGE);
                        serviceIntent.putExtra(MessageTransferService.EXTRA_MESSAGE_CONTENT, message);

                        //serviceIntent.putExtra(MessageTransferService.EXTRAS_DESTINATION_ADDRESS,
                        //        info.groupOwnerAddress.getHostAddress());
                        serviceIntent.putExtra(MessageTransferService.EXTRAS_DESTINATION_ADDRESS,
                                        destAddr.getHostAddress());

                        serviceIntent.putExtra(MessageTransferService.EXTRAS_DESTINATION_PORT, 8988);
                        getActivity().startService(serviceIntent);

                        //statusText.setText("Message 2: " + message.getMessageContent());

                        /*Toast.makeText(getActivity(), "Message should sent",
                                Toast.LENGTH_SHORT).show();*/

                    }
                });

        return mContentView;
    }

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.

        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }*/

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;

        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.

        Toast.makeText(getActivity(), "Conn info avail",
                Toast.LENGTH_SHORT).show();

        // TODO: Modified to let every node setting up server
        if (info.groupFormed) {
            if (!info.isGroupOwner) {
                destAddr = info.groupOwnerAddress;

                Intent serviceIntent = new Intent(getActivity(), MessageTransferService.class);


                Message message = new Message();
                message.setIsInit(true);

                serviceIntent.setAction(MessageTransferService.ACTION_SEND_MESSAGE);
                serviceIntent.putExtra(MessageTransferService.EXTRA_MESSAGE_CONTENT, message);

                //serviceIntent.putExtra(MessageTransferService.EXTRAS_DESTINATION_ADDRESS,
                //        info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(MessageTransferService.EXTRAS_DESTINATION_ADDRESS,
                        destAddr.getHostAddress());

                serviceIntent.putExtra(MessageTransferService.EXTRAS_DESTINATION_PORT, 8988);

                MessageTransferService.fragment = this;

                getActivity().startService(serviceIntent);

                Log.v(WiFiDirectActivity.TAG, "Sending initial message to " + destAddr.getHostAddress());

                Toast.makeText(getActivity(), destAddr.getHostAddress(),
                        Toast.LENGTH_SHORT).show();
            }
            //else {
                new MessageServerAsyncTask(getActivity(),
                        mContentView.findViewById(R.id.status_text),
                        new AlertDialog.Builder(getActivity()),
                        destAddr)
                        .execute();
            //}

            // The other device acts as the client. In this case, we enable the
            // get file button.
            // NOTE: No longer valid

            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            //((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
            //        .getString(R.string.client_text));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public class MessageServerAsyncTask extends AsyncTask<Void, Void, Message> {

        private Context context;
        private TextView statusText;
        private AlertDialog.Builder dialog;
        private InetAddress destAddr;

        /**
         * @param context
         * @param statusText
         */
        public MessageServerAsyncTask(Context context, View statusText,
                                      AlertDialog.Builder dialog, InetAddress destAddr) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.dialog = dialog;
            this.destAddr = destAddr;
        }

        @Override
        protected Message doInBackground(Void... params) {
            try {

                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();


                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                /*final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();*/

                InputStream in = client.getInputStream();
                ObjectInputStream inputStream = new ObjectInputStream(in);
                Message message = (Message) inputStream.readObject();

                in.close();
                inputStream.close();
                serverSocket.close();

                // record the address
                if (message != null) {
                    if (message.getIsInit()) {
                        destAddr = client.getInetAddress();

                        Log.v(WiFiDirectActivity.TAG, "Receive initial message");
                        Log.v(WiFiDirectActivity.TAG, destAddr.getHostAddress());

                    }
                }

                return message;

            } catch (Exception e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Message result) {
            if (result != null) {
                statusText.setText("Message received - " + result.getMessageContent());

                if (!result.getIsInit())  {
                    dialog.setMessage(result.getMessageContent())
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //
                                }
                            });
                    dialog.show();
                }

                ((WiFiDirectActivity)getActivity()).onResume();
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

    /*public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();
        
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;
            Log.v("","Time taken to transfer all bytes is : "+endTime);
            
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }*/



}
