package vincente.com.multidownloadbluetooth;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by vincente on 4/26/16
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder>{
    private static final int VIEW_TYPE_ME = 0;
    private static final int VIEW_TYPE_THEM = 1;
    private Context context;

    public MessageAdapter(Context context){
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mView;
        switch(viewType){
            case VIEW_TYPE_ME:
                mView = View.inflate(context, R.layout.list_item_message_me, null);
                break;
            case VIEW_TYPE_THEM:
                mView = View.inflate(context, R.layout.list_item_message_them, null);
                break;
            default:
                mView = null;
        }
        ViewHolder viewHolder = new ViewHolder(mView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }



    @Override
    public int getItemCount() {
        return 0;
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
