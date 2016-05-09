package vincente.com.multidownloadbluetooth.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import vincente.com.multidownloadbluetooth.CursorRecyclerViewAdapter;
import vincente.com.multidownloadbluetooth.DbHelper;
import vincente.com.multidownloadbluetooth.R;

/**
 * Created by vincente on 4/26/16
 */
public class MessageAdapter extends CursorRecyclerViewAdapter<MessageAdapter.ViewHolder> {
    private static final int VIEW_TYPE_ME = 0;
    private static final int VIEW_TYPE_THEM = 1;
    private Context context;

    private static int CUR_IDX_ID;

    private static int CUR_IDX_BODY;
    private static int CUR_IDX_TIMESTAMP;
    private static int CUR_IDX_SENT_FROM_ME;
    public MessageAdapter(Context context, Cursor cursor){
        super(context, cursor);
        this.context = context;

        CUR_IDX_ID = cursor.getColumnIndex(DbHelper.KEY_ID);
        CUR_IDX_BODY = cursor.getColumnIndex(DbHelper.KEY_BODY);
        CUR_IDX_TIMESTAMP = cursor.getColumnIndex(DbHelper.KEY_TIMESTAMP);
        CUR_IDX_SENT_FROM_ME = cursor.getColumnIndex(DbHelper.KEY_SENT_FROM_ME);
    }

    @Override
    public long getItemId(Cursor cursor) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mView;
        switch(viewType){
            case VIEW_TYPE_ME:
                mView = View.inflate(context, R.layout.list_item_message_me, null);
                break;
            case VIEW_TYPE_THEM:
            default:
                mView = View.inflate(context, R.layout.list_item_message_them, null);
                break;
        }

        return new ViewHolder(mView);
    }

    @Override
    public int getItemViewType(Cursor cursor) {
        if(cursor.getInt(CUR_IDX_SENT_FROM_ME)==1){
            return VIEW_TYPE_ME;
        }
        else{
            return VIEW_TYPE_THEM;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        holder.tvBody.setText(cursor.getString(CUR_IDX_BODY));
        holder.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(Long.valueOf(String.valueOf(cursor.getLong(CUR_IDX_TIMESTAMP)))));
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public TextView tvBody;
        public TextView tvTimestamp;
        public ViewHolder(View itemView) {
            super(itemView);
            this.tvBody = (TextView) itemView.findViewById(R.id.tv_body);
            this.tvTimestamp = (TextView) itemView.findViewById(R.id.tv_timestamp);
        }
    }
}
