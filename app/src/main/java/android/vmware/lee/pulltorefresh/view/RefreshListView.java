package android.vmware.lee.pulltorefresh.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.vmware.lee.pulltorefresh.R;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chuanl on 12/22/14.
 */
public class RefreshListView extends ListView implements AbsListView.OnScrollListener{
    private View header;
    private int headerHeight;
    private int firstVisibleItem;
    private int scrollState;

    private boolean isRemark; //whether we pull the listView on the top of the listView or not?
    private int startY; //the start Y value when we pull the listView;

    private int state;
    final int NONE = 0;  // normal state
    final int PULL = 1;  // pull to refresh state
    final int RELEASE = 2;  //release to refresh state
    final int RELEASING = 3;   // refreshing state

    private IRefreshListener iRefreshListener;

    public RefreshListView(Context context) {
        super(context);
        initView(context);
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        header = inflater.inflate(R.layout.header_layout, null);
        measureView(header);
        headerHeight = header.getMeasuredHeight();
        setTopPadding(-headerHeight);
        this.addHeaderView(header);
        super.setOnScrollListener(this);
    }

    /**
     * Notify the parent view, and measure the view
     * @param view
     */
    private void measureView(View view){
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if(lp == null){
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int width = ViewGroup.getChildMeasureSpec(0, 0, lp.width);
        int height;
        int tempHeight = lp.height;
        if(tempHeight > 0){
            height = MeasureSpec.makeMeasureSpec(tempHeight, MeasureSpec.EXACTLY);
        }else{
            height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        view.measure(width, height);
    }

    /**
     * Set the top padding of the header
     * @param topPadding
     */
    private void setTopPadding(int topPadding) {
        header.setPadding(header.getPaddingLeft(), topPadding, header.getPaddingRight(), header.getPaddingBottom());
        header.invalidate();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.scrollState = scrollState;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        this.firstVisibleItem = firstVisibleItem;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(firstVisibleItem == 0){
                    isRemark = true;
                    startY = (int)ev.getY();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                onMove(ev);
                break;
            case MotionEvent.ACTION_UP:
                if(state == RELEASE){
                    state = RELEASING;
                    // refreshing action
                    redrawViewByState();
                    iRefreshListener.onRefrsh();
                }else if(state == PULL){
                    state = NONE;
                    isRemark = false;
                    redrawViewByState();
                }
                break;
        }
        return super.onTouchEvent(ev);
    }


    private void onMove(MotionEvent ev){
        if(!isRemark)
            return;
        int tempY = (int)ev.getY();
        int space = tempY - startY;
        int topPadding = space - headerHeight;
//        Log.d("TAG", "start: " + startY + "  tempY: "+ tempY);
        Log.d("TAG", "Space: " + space);
        switch (state){
            case NONE:
                if(space > 0){
                    state = PULL;
                    redrawViewByState();
                }
                break;
            case PULL:
                setTopPadding(topPadding);
                if(space > headerHeight + 30 &&
                        scrollState == SCROLL_STATE_TOUCH_SCROLL){
                    state = RELEASE;
                    redrawViewByState();
                }
                break;
            case RELEASE:
                setTopPadding(topPadding);
                if(space < headerHeight + 30){
                    state = PULL;
                    redrawViewByState();
                }else if(space <= 0){
                    state = NONE;
                    isRemark = false;
                    redrawViewByState();
                }
                break;
            case RELEASING:
                break;
        }

    }

    private void redrawViewByState(){
        TextView tip = (TextView)header.findViewById(R.id.tip);
        ImageView arrow = (ImageView)header.findViewById(R.id.arrow);
        ProgressBar progress = (ProgressBar)header.findViewById(R.id.progress);
        RotateAnimation anim0 = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        anim0.setDuration(500);
        anim0.setFillAfter(true);
        RotateAnimation anim1 = new RotateAnimation(180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        anim1.setDuration(500);
        anim1.setFillAfter(true);
        switch (state){
            case NONE:
                setTopPadding(-headerHeight);
                arrow.clearAnimation();
                break;
            case PULL:
                arrow.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                tip.setText("Pull to refresh");
                arrow.clearAnimation();
                arrow.setAnimation(anim1);
                break;
            case RELEASE:
                arrow.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                tip.setText("release to refresh");
                arrow.clearAnimation();
                arrow.setAnimation(anim0);
                break;
            case RELEASING:
                setTopPadding(20);
                arrow.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                tip.setText("refreshing...");
                arrow.clearAnimation();
                break;
        }
    }

    public void refreshComplete(){
        state = NONE;
        isRemark = false;
        redrawViewByState();
        TextView lastUpdateTime = (TextView)header.findViewById(R.id.lastUpdateTime);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String time = format.format(date);
        lastUpdateTime.setText(time);
    }

    public void setRefreshListener(IRefreshListener listener){
        iRefreshListener = listener;
    }

    public interface IRefreshListener{
        public void onRefrsh();
    }
}
