package vincente.com.multidownloadbluetooth.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.UUID;

import vincente.com.multidownloadbluetooth.CursorRecyclerViewAdapter;
import vincente.com.multidownloadbluetooth.DbHelper;
import vincente.com.multidownloadbluetooth.R;
import vincente.com.multidownloadbluetooth.ThreadActivity;
import vincente.com.pnib.Config;

/**
 * Created by vincente on 4/26/16
 */
public class ContactAdapter extends CursorRecyclerViewAdapter<ContactAdapter.ViewHolder> {
    private static final int VIEW_TYPE_CONTACT = 0;

    private Context context;

    public ContactAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        this.context = context;
    }

    @Override
    public long getItemId(Cursor cursor) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View mView;
        switch(viewType){
            case VIEW_TYPE_CONTACT:
            default:
                mView = View.inflate(context, R.layout.list_item_contact, null);
                break;
        }

        return new ViewHolder(mView);
    }

    @Override
    public int getItemViewType(Cursor cursor) {
        return VIEW_TYPE_CONTACT;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        int CUR_IDX_ID = cursor.getColumnIndex(DbHelper.KEY_ID);
        int CUR_IDX_UUID = cursor.getColumnIndex(DbHelper.KEY_UUID);
        int CUR_IDX_NICKNAME = cursor.getColumnIndex(DbHelper.KEY_NICKNAME);
        int CUR_IDX_IN_RANGE = cursor.getColumnIndex(DbHelper.KEY_IN_RANGE);

        String nameFormat = "%s [%s]";
        String nickname = cursor.getString(CUR_IDX_NICKNAME);
        holder.uuid = cursor.getString(CUR_IDX_UUID);
        holder.tvName.setText(String.format(
                nameFormat,
                nickname == null ? "" : nickname,
                UUID.nameUUIDFromBytes(Config.bytesFromString(holder.uuid))
        ));

//        imageView.setColorFilter(main.getResources().getColor(R.color.blue), android.graphics.PorterDuff.Mode.MULTIPLY);

        if(cursor.getInt(CUR_IDX_IN_RANGE)==1){
            holder.ivContactPhoto.setColorFilter(android.R.color.holo_red_dark, PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.ivContactPhoto.setColorFilter(null);
        }
        holder.itemView.setOnClickListener(holder);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public String uuid;
        public TextView tvName;
        public ImageView ivContactPhoto;
        public ViewHolder(View itemView) {
            super(itemView);
            this.tvName= (TextView) itemView.findViewById(R.id.tv_name);
            this.ivContactPhoto = (ImageView) itemView.findViewById(R.id.iv_contact_photo);
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(context, ThreadActivity.class);
            i.putExtra("uuid", uuid);
            context.startActivity(i);
        }
    }
}
