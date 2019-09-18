/*
 * Copyright feelSpace GmbH, 2016-2018
 *
 * @author David Meignan
 */
package de.feelspace.fsdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import de.feelspace.fslib.BeltConnectionInterface;
import de.feelspace.fslib.BeltConnectionListener;
import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.R;

/**
 * A dialog for scan result and selection.
 */
public class SearchDialogFragment extends DialogFragment
        implements AdapterView.OnItemClickListener, BeltConnectionListener {

    // Belt selection callback
    private OnBeltSelectedCallback selectionCallback;

    // Context of the dialog
    private Context context;

    // Belt connection manager
    private BeltConnectionInterface beltConnectionManager;

    // Progress indicator
    private ProgressBar progressBar;

    // List adapter for devices
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private BTDeviceArrayAdapter deviceListAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.context = context;
            // Retrieve callback
            selectionCallback = (OnBeltSelectedCallback) context;
            // Set connection manager
            beltConnectionManager = MainActivity.getBeltConnection();
            beltConnectionManager.addConnectionListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()+" must implement " +
                    "OnBeltSelectedCallback.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain instance on rotation
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") // This is an exception on AlertDialog. The parent view is
                // not known at this step, so 'null' is given as second parameter for 'inflate'.
        View contentView = inflater.inflate(R.layout.scan_dialog, null);
        builder.setView(contentView);
        // List
        ListView deviceListView = contentView.findViewById(R.id.scan_dialog_device_list_view);
        deviceListAdapter = new BTDeviceArrayAdapter(getActivity(), deviceList);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener(this);
        // Title
        builder.setTitle(R.string.scan_dialog_title_text);
        // Cancel button
        builder.setNegativeButton(R.string.scan_dialog_cancel_button_text,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });
        // Search progress
        progressBar = contentView.findViewById(R.id.scan_dialog_progress_bar);
        // Start scan
        if (beltConnectionManager != null) {
            beltConnectionManager.scan();
        }
        // Create dialog
        return builder.create();
    }

    /**
     * Updates the visibility of the progress bar.
     */
    private void updateProgressBar() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressBar != null && beltConnectionManager != null) {
                        if (beltConnectionManager.getState() ==
                                BeltConnectionState.STATE_SCANNING) {
                            progressBar.setVisibility(View.VISIBLE);
                        } else {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        // Handles https://code.google.com/p/android/issues/detail?id=17423
        // A beautiful bug with plenty of chocolate chips in it.
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        // Stop scan
        if (beltConnectionManager != null) {
            beltConnectionManager.removeConnectionListener(this);
            if (beltConnectionManager.getState() == BeltConnectionState.STATE_SCANNING) {
                beltConnectionManager.stopScan();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = deviceListAdapter.getItem(position);
        if (device != null) {
            selectionCallback.onBeltSelected(device);
            dismiss();
        }
    }

    @Override
    public void onScanFailed() {
        // Nothing to do
    }

    @Override
    public void onNoBeltFound() {
        // Nothing to do
    }

    @Override
    public void onBeltFound(@NonNull BluetoothDevice device) {
        deviceListAdapter.add(device);
    }

    @Override
    public void onConnectionStateChange(BeltConnectionState state) {
        updateProgressBar();
    }

    @Override
    public void onConnectionLost() {
        // Nothing to do
    }

    @Override
    public void onConnectionFailed() {
        // Nothing to do
    }

    /**
     * Array adapter for the list of devices.
     */
    private class BTDeviceArrayAdapter extends ArrayAdapter<BluetoothDevice> {

        /**
         * Constructor.
         *
         * @param context the context.
         * @param list the initial list of devices.
         */
        BTDeviceArrayAdapter(Context context, ArrayList<BluetoothDevice> list) {
            super(context, -1, list);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            BluetoothDevice device = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.scan_dialog_device_item_view, parent, false);
            }
            // Lookup view for data population
            TextView deviceNameTextView = (TextView) convertView.findViewById(
                    R.id.bluetooth_device_item_view_device_name);
            // Populate the data into the template view using the data object
            String deviceName = (device == null)?(""):(device.getName());
            if (deviceName == null) {
                deviceName = device.getAddress();
            }
            deviceNameTextView.setText(deviceName);
            // Return the completed view to render on screen
            return convertView;
        }
    }

    /**
     * Callback interface for belt selection.
     */
    public interface OnBeltSelectedCallback {

        /**
         * Called when a belt has been selected in the dialog.
         *
         * @param device The belt selected.
         */
        void onBeltSelected(@NonNull BluetoothDevice device);
    }
}
