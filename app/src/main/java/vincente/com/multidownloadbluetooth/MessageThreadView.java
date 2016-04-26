package vincente.com.multidownloadbluetooth;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by vincente on 4/20/16
 */
public class MessageThreadView extends Fragment{

    private EditText etMessage;
    private Button btnSend;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = View.inflate(getContext(), R.layout.fragment_thread, null);

        recyclerView = (RecyclerView) view.findViewById(R.id.rv_messages);
        etMessage = (EditText) view.findViewById(R.id.et_message);
        btnSend = (Button) view.findViewById(R.id.btn_send);

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
        return view;
    }


    public void send(String text){

    }
}
