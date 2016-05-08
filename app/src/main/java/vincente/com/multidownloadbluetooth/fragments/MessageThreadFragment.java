package vincente.com.multidownloadbluetooth.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.devspark.progressfragment.ProgressFragment;

import vincente.com.multidownloadbluetooth.DBUtils;
import vincente.com.multidownloadbluetooth.adapters.MessageAdapter;
import vincente.com.multidownloadbluetooth.R;
import vincente.com.pnib.BluetoothLeService;

/**
 * Created by vincente on 4/20/16
 */
public class MessageThreadFragment extends ProgressFragment implements ServiceConnection{
    private static final String KEY_OTHER_ADDRESS = "other_address";
    private EditText etMessage;
    private Button btnSend;
    private RecyclerView recyclerView;
    private View mContentView;
    private Handler mHandler;
    private String otherAddress;
    private AsyncTask<Void, Void, Cursor> getMessagesAsyc;
    private BluetoothLeService sendingService;

    public static MessageThreadFragment createInstance(String otherAddress){
        Bundle args = new Bundle();
        args.putString(KEY_OTHER_ADDRESS, otherAddress);
        MessageThreadFragment fragment = new MessageThreadFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public MessageThreadFragment(){}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentView(mContentView);
        setEmptyText("No Messages Found");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        otherAddress = arguments.getString(KEY_OTHER_ADDRESS);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = View.inflate(getContext(), R.layout.fragment_thread, null);

        recyclerView = (RecyclerView) mContentView.findViewById(R.id.rv_messages);
        etMessage = (EditText) mContentView.findViewById(R.id.et_message);
        btnSend = (Button) mContentView.findViewById(R.id.btn_send);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(etMessage.getText().toString());
            }
        });
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                etMessage.setEnabled(!s.toString().isEmpty());
            }
        });

        // Show indeterminate progress
        obtainData();
        Intent i = new Intent(getContext(), BluetoothLeService.class);
        getContext().bindService(i, this, Context.BIND_AUTO_CREATE);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void obtainData(){
        if(getMessagesAsyc != null && !getMessagesAsyc.isCancelled()){
            getMessagesAsyc.cancel(true);
        }

        getMessagesAsyc = new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                return DBUtils.getMessages(getContext(), otherAddress);
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if(recyclerView.getAdapter() != null){
                    ((MessageAdapter) recyclerView.getAdapter()).getCursor().close();
                }
                recyclerView.setAdapter(new MessageAdapter(getContext(), cursor));
                setContentShown(true);
            }
        }.execute();
    }

    public void send(String text){
        Toast.makeText(getContext(), "Sending Message: " + text, Toast.LENGTH_SHORT).show();
        if(sendingService != null){
            sendingService.sendMessage(otherAddress, text);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
        sendingService = binder.getSendingServiceInstance();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        sendingService = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent i = new Intent(getContext(), BluetoothLeService.class);
        getContext().bindService(i, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        getContext().unbindService(this);
        super.onStop();
    }
}
