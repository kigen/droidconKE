package droiddevelopers254.droidconke.views.activities;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.bottomappbar.BottomAppBar;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import droiddevelopers254.droidconke.R;
import droiddevelopers254.droidconke.adapters.SpeakersAdapter;
import droiddevelopers254.droidconke.models.RoomModel;
import droiddevelopers254.droidconke.models.SessionsModel;
import droiddevelopers254.droidconke.models.SpeakersModel;
import droiddevelopers254.droidconke.models.StarredSessionModel;
import droiddevelopers254.droidconke.viewmodels.SessionDataViewModel;

public class SessionViewActivity extends AppCompatActivity {
    BottomAppBar bottomAppBar;
    int sessionId,roomId;
    FloatingActionButton fab;


    @BindView(R.id.txtSessionTime)
    TextView txtSessionTime;
    @BindView(R.id.txtSessionRoom)
    TextView txtSessionRoom;
    @BindView(R.id.txtSessionDesc)
    TextView txtSessionDesc;
    @BindView(R.id.txtSessionCategory)
    TextView txtSessionCategory;
    @BindView(R.id.sessionViewTitleText)
    TextView sessionViewTitleText;
    @BindView(R.id.bottomSheetView)
    View bottomSheetView;
    @BindView(R.id.collapseBottomImg)
    ImageView collapseBottomImg;
    @BindView(R.id.speakersRV)
    RecyclerView recyclerView;
    @BindView(R.id.speakersLinear)
    LinearLayout speakersLinear;
    @BindView(R.id.roomDetailsText)
    TextView roomDetailsText;

    SessionDataViewModel sessionDataViewModel;
    private BottomSheetBehavior bottomSheetBehavior;
    String starStatus,dayNumber;
    SessionsModel sessionsModel1;
    private DatabaseReference databaseReference;
    List<SpeakersModel> speakersList= new ArrayList<>();
    List<Integer> speakerId =new ArrayList<>();
    SpeakersAdapter speakersAdapter;
    static RecyclerView.LayoutManager mLayoutManager;
    StarredSessionModel starredSessionModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_view);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        starredSessionModel = new StarredSessionModel();

        //get extras
        Intent extraIntent=getIntent();
        sessionId = extraIntent.getIntExtra("sessionId",0);
        dayNumber=extraIntent.getStringExtra("dayNumber");
        starStatus=extraIntent.getStringExtra("starred");
        speakerId= extraIntent.getIntegerArrayListExtra("speakerId");
        roomId=extraIntent.getIntExtra("roomId",0);

        ButterKnife.bind(this);

        sessionDataViewModel= ViewModelProviders.of(this).get(SessionDataViewModel.class);
        bottomSheetBehavior= BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // this part hides the button immediately and waits bottom sheet
                // to collapse to show
                if (BottomSheetBehavior.STATE_EXPANDED == newState) {
                    fab.animate().scaleX(0).scaleY(0).setDuration(200).start();
                } else if (BottomSheetBehavior.STATE_COLLAPSED == newState) {
                    fab.animate().scaleX(1).scaleY(1).setDuration(200).start();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSessionData(dayNumber,sessionId);

        for (Integer i : speakerId){
            getSpeakerDetails(i);
        }

        getRoomDetails(roomId);
        //observe live data emitted by view model
        sessionDataViewModel.getSessionDetails().observe(this,sessionDataState -> {
            if (sessionDataState.getDatabaseError() != null){
                handleDatabaseError(sessionDataState.getDatabaseError());
            }else {
                handleFetchSessionData(sessionDataState.getSessionsModel());
            }
        });

        sessionDataViewModel.getSpeakerInfo().observe(this,speakersState -> {
            if (speakersState.getDatabaseError() != null){
                handleDatabaseError(speakersState.getDatabaseError());
            }else {
                handleFetchSpeakerDetails(speakersState.getSpeakersModel());
            }
        });

        sessionDataViewModel.getRoomInfo().observe(this,roomState -> {
            if (roomState.getDatabaseError() != null){
                handleDatabaseError(roomState.getDatabaseError());
            }else {
                handleFetchRoomDetails(roomState.getRoomModel());
            }
        });

        bottomAppBar=findViewById(R.id.bottomAppBar);
        sessionViewTitleText=findViewById(R.id.sessionViewTitleText);
        fab=findViewById(R.id.fab);
        bottomAppBar.replaceMenu(R.menu.menu_bottom_appbar);

        //check a session was previously starred
//        if (starStatus.equals("0")){
//            fab.setImageResource(R.drawable.ic_star_border_black_24dp);
//        }else if (starStatus.equals("1")){
//            fab.setImageResource(R.drawable.ic_star_blue_24dp);
//        }

        //handle menu items on material bottom bar
        bottomAppBar.setOnMenuItemClickListener(item -> {
            int id= item.getItemId();
            if (id == R.id.action_share){
                Intent shareSession=new Intent();
                shareSession.setAction(Intent.ACTION_SEND);
                shareSession.putExtra(Intent.EXTRA_TEXT,"Check out "+"'"+ sessionId +"' at "+getString(R.string.droidcoke_hashtag)+"\n"+getString(R.string.droidconke_site));
                shareSession.setType("text/plain");
                startActivity(shareSession);
            }
            if (id == R.id.action_map){
//                //animate fab to display on top of the bottom sheet
//                CoordinatorLayout.LayoutParams layoutParams= (CoordinatorLayout.LayoutParams)fab.getLayoutParams();
//                layoutParams.setAnchorId(R.id.bottomSheetView);
//
//                fab.setLayoutParams(layoutParams);

                if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                }
            }
            return false;
        });

        //star a session
        fab.setOnClickListener(view -> {
            if (starStatus.equals("0")){
                //start a session
               fab.setImageResource(R.drawable.ic_star_blue_24dp);

               starredSessionModel.setDay(dayNumber);
               starredSessionModel.setSession_id(String.valueOf(sessionsModel1.getId()));
               starredSessionModel.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

               databaseReference.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                       .child("starred").push().setValue(starredSessionModel);

               //this will aid in tracking every starred session and then send a push notification
               databaseReference.child("starred_sessions").push().setValue(starredSessionModel);

                //update in firebase
              //  databaseReference.child(dayNumber).child(String.valueOf(sessionsModel1.getId())).child("starred").setValue("1");

            }else if(starStatus.equals("1")){
                fab.setImageResource(R.drawable.ic_star_border_black_24dp);

                //update in firebase
               // databaseReference.child(dayNumber).child(String.valueOf(sessionsModel1.getId())).child("starred").setValue("0");
            }
        });
        //collapse bottom bar
        collapseBottomImg.setOnClickListener(view -> {
            if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

    }

    private void getRoomDetails(int roomId) {
        sessionDataViewModel.fetchRoomDetails(roomId);
    }
    private void handleFetchRoomDetails(RoomModel roomModel) {
        if (roomModel != null){
            roomDetailsText.setText(roomModel.getName() + "Room capacity is: "+String.valueOf(roomModel.getCapacity()));
        }
    }
    private void getSpeakerDetails(int speakerId) {
        sessionDataViewModel.fetchSpeakerDetails(speakerId);
    }

    private void handleFetchSpeakerDetails(List<SpeakersModel> speakersModel) {
        if (speakersModel != null){
            speakersList= speakersModel;
            initView();
        }else {
            //if there are no speakers for this session hide views
            speakersLinear.setVisibility(View.GONE);
        }
    }

    private void initView() {
        speakersAdapter= new SpeakersAdapter(speakersList,getApplicationContext());
        mLayoutManager= new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(speakersAdapter);
    }

    private void handleFetchSessionData(SessionsModel sessionsModel) {
        if (sessionsModel != null){
            sessionsModel1=sessionsModel;
            //set the data on the view
            txtSessionTime.setText(sessionsModel.getTime());
            txtSessionRoom.setText(sessionsModel.getRoom());
            txtSessionDesc.setText(sessionsModel.getDescription());
            txtSessionCategory.setText(sessionsModel.getTopic());
            sessionViewTitleText.setText(sessionsModel.getTitle());
        }
    }

    private void handleDatabaseError(String databaseError) {
        Toast.makeText(getApplicationContext(),databaseError,Toast.LENGTH_SHORT).show();
    }

    private void getSessionData(String dayNumber, int sessionId){
        sessionDataViewModel.fetchSessionDetails(dayNumber,sessionId);
    }

}
