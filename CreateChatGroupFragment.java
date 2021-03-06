package com.example.shroomies;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CreateChatGroupFragment extends DialogFragment {

    private View v;
    private SearchView searchView;
    private RecyclerView userListSuggestionRecyclerView;
    private ImageButton nextButton;
    private DatabaseReference rootRef;
    private UserRecyclerAdapter userRecyclerAdapter;
    private String groupID;
    private boolean fromGroupInfo;
    private FirebaseAuth mAuth;
    private  List <User> suggestedUser;
    private List<String> inboxListUsers;
    static ArrayList<User>  selectedMembers;



    @Override
    public void onStart() {
        super.onStart();
        if(getDialog()!=null) {
            getDialog().getWindow().setLayout(ActionBar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
         getDialog().getWindow().setBackgroundDrawableResource(R.drawable.create_group_fragment_background);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v= inflater.inflate(R.layout.fragment_create_chat_group, container, false);
        rootRef= FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();


        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setWindowAnimations(R.style.DialogAnimation);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchView=v.findViewById(R.id.create_group_search_bar);

        userListSuggestionRecyclerView =v.findViewById(R.id.suggestion_list_create_group);
        userListSuggestionRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        nextButton=v.findViewById(R.id.confirm_button_create_group);




        Bundle extras=getArguments();
        if(extras!=null) {
            fromGroupInfo = extras.getBoolean("FromGroupInfo");
            groupID = extras.getString("GROUPID");

        }
        selectedMembers = new ArrayList<>();
        getMessageInboxListIntoAdapter();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getUsersListFromDatabase(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {

                getMessageInboxListIntoAdapter();
                return false;
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fromGroupInfo){
                    Intent intent = new Intent(getContext(),GroupInfoActivity.class);
                    intent.putExtra("GROUPID" , groupID);
                    intent.putParcelableArrayListExtra("ListOfSelectedUsers",selectedMembers);
                    startActivity(intent);
                }
                    CreateChatGroupDialogFrag2 dialogFrag2=new CreateChatGroupDialogFrag2();
                    Bundle bundle= new Bundle();
                    bundle.putParcelableArrayList("ListOfSelectedUsers",selectedMembers);
                    dialogFrag2.setArguments(bundle);
                    dialogFrag2.show(getChildFragmentManager(),"create group dialog 2");
            }
        });

    }

    private void getMessageInboxListIntoAdapter() {
        inboxListUsers = new ArrayList<>();
        rootRef.child("PrivateChatList").child(mAuth.getInstance().getCurrentUser().getUid()).orderByChild("receiverID").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot dataSnapshot
                    :snapshot.getChildren()){
                        HashMap<String,String> recievers= (HashMap) dataSnapshot.getValue();
                        inboxListUsers.add(recievers.get("receiverID"));
                    }
                    addInboxUsersToRecycler(inboxListUsers);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void addInboxUsersToRecycler(final List<String> inboxListUsers) {
        suggestedUser = new ArrayList<>();
        suggestedUser.clear();
        suggestedUser.addAll(selectedMembers);
        userRecyclerAdapter=new UserRecyclerAdapter(suggestedUser,getContext(),"SEARCH_PAGE"  , selectedMembers);
        userListSuggestionRecyclerView.setAdapter(userRecyclerAdapter);
        for(String id
        :inboxListUsers){
                    rootRef.child("Users").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                User user = snapshot.getValue(User.class);
                                boolean userFound = false;
                                for(User mUser
                                        :suggestedUser){
                                    if(mUser.getID().equals(user.getID())){
                                        userFound=true;
                                    }
                                }

                                if(!userFound){
                                    suggestedUser.add(user);
                                }


                            }
                            userRecyclerAdapter.notifyDataSetChanged();

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }
    }

    public void getUsersListFromDatabase(String query){
        suggestedUser = new ArrayList<>();
        userRecyclerAdapter=new UserRecyclerAdapter(suggestedUser,getContext(),"SEARCH_PAGE");
        userListSuggestionRecyclerView.setAdapter(userRecyclerAdapter);

        //+"\uf8ff"
        rootRef.child("Users").orderByChild("name").startAt(query)
                .endAt(query+"\uf8ff").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        User user = ds.getValue(User.class);
                        suggestedUser.add(user);
                    }
                    //add the members already selected
                    userRecyclerAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public static void addSelectedMembers(User user){

       if(!selectedMembers.contains(user)){
           selectedMembers.add(user);
        }else{
           // add member already selected exception
       }

    }
    public static void removeSelectedMember(User user) {
        selectedMembers.remove(user);
    }

}