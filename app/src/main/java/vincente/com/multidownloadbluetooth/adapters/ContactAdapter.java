package vincente.com.multidownloadbluetooth.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import vincente.com.multidownloadbluetooth.DbHelper;
import vincente.com.multidownloadbluetooth.R;

/**
 * Created by vincente on 4/26/16
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder>{
    private static final int VIEW_TYPE_CONTACT = 0;
    private Context context;
    private Cursor cursor;

    public ContactAdapter(Context context, Cursor cursor){
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mView;
        switch(viewType){
            case VIEW_TYPE_CONTACT:
            default:
                mView = View.inflate(context, R.layout.list_item_contact, null);
                break;
        }

        return new ViewHolder(mView);
    }

    @Override
    public int getItemViewType(int position) {
        cursor.moveToPosition(position);
        return VIEW_TYPE_CONTACT;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        int CUR_IDX_ID = cursor.getColumnIndex(DbHelper.KEY_ID);
        int CUR_IDX_ADDRESS = cursor.getColumnIndex(DbHelper.KEY_ADDRESS);
        int CUR_IDX_NICKNAME = cursor.getColumnIndex(DbHelper.KEY_NICKNAME);

        String nameFormat = "%s [%s]";
        String nickname = cursor.getString(CUR_IDX_NICKNAME);
        String address = cursor.getString(CUR_IDX_ADDRESS);

        holder.tvName.setText(String.format(
                nameFormat,
                nickname == null ? "" : nickname,
                address
        ));

        holder.ivContactPhoto.setImageResource(R.mipmap.ic_launcher);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public TextView tvName;
        public ImageView ivContactPhoto;
        public ViewHolder(View itemView) {
            super(itemView);
            this.tvName= (TextView) itemView.findViewById(R.id.tv_name);
            this.ivContactPhoto = (ImageView) itemView.findViewById(R.id.iv_contact_photo);
        }
    }
}
