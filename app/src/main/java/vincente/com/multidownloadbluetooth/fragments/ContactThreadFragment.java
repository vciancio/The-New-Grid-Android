package vincente.com.multidownloadbluetooth.fragments;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devspark.progressfragment.ProgressFragment;

import vincente.com.multidownloadbluetooth.DBUtils;
import vincente.com.multidownloadbluetooth.R;
import vincente.com.multidownloadbluetooth.adapters.ContactAdapter;

/**
 * Created by vincente on 5/4/16
 */
public class ContactThreadFragment extends ProgressFragment {

    private RecyclerView mContentView;

    public static ContactThreadFragment createInstance(){
        ContactThreadFragment fragment = new ContactThreadFragment();
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setContentView(mContentView);
        setEmptyText("We've never seen another device...");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = (RecyclerView) View.inflate(getContext(), R.layout.fragment_devices, null);
        mContentView.setLayoutManager(new LinearLayoutManager(getContext()));
        obtainData();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void obtainData(){
        new AsyncTask<Void, Void, Cursor>(){

            @Override
            protected Cursor doInBackground(Void... params) {
                return DBUtils.getUsers(getContext());
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                super.onPostExecute(cursor);
                mContentView.setAdapter(new ContactAdapter(getContext(), cursor));
                setContentShown(true);
            }
        }.execute();
    }
}
