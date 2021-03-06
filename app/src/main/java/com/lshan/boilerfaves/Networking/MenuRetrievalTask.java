package com.lshan.boilerfaves.Networking;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.lshan.boilerfaves.Activities.MainActivity;
import com.lshan.boilerfaves.Adapters.FoodAdapter;
import com.lshan.boilerfaves.Models.DiningCourtModel;
import com.lshan.boilerfaves.Models.DiningCourtWrapperModel;
import com.lshan.boilerfaves.Models.FoodModel;
import com.lshan.boilerfaves.Models.DiningCourtMenu;
import com.lshan.boilerfaves.Models.MenuModel;
import com.lshan.boilerfaves.Utils.NotificationHelper;
import com.lshan.boilerfaves.Utils.SharedPrefsHelper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import retrofit2.Response;

/**
 * Created by lshan on 1/4/2018.
 */

public class MenuRetrievalTask extends AsyncTask<Void, Void, ArrayList<DiningCourtMenu>>{

    private static final String API_URL = "https://api.hfs.purdue.edu/menus/v1/locations/";
    private RecyclerView mainRecyclerView;
    private int notificationType;
    private Context context;
    RelativeLayout progressLayout;
    RelativeLayout noAvailableFavesLayout;
    RelativeLayout noFavesLayout;
    FrameLayout frameLayout;
    private MainActivity activity;

    public static final int NO_NOTIFICATION = 0;
    public static final int BREAKFAST_NOTIFICATION = 1;
    public static final int LUNCH_NOTIFICATION = 2;
    public static final int DINNER_NOTIFICATION = 3;

    public MenuRetrievalTask(Context context, RecyclerView mainRecyclerView, RelativeLayout progressLayout, FrameLayout frameLayout, int notificationType){
        this.context = context;
        this.mainRecyclerView = mainRecyclerView;
        this.notificationType = notificationType;
        this.progressLayout = progressLayout;
        this.frameLayout = frameLayout;
    }

    //TODO change this to just use the reference to the activity
    public MenuRetrievalTask(Context context, RecyclerView mainRecyclerView, RelativeLayout progressLayout, FrameLayout frameLayout, int notificationType, RelativeLayout noAvailableFavesLayout, RelativeLayout noFavesLayout, MainActivity activity){
        this.context = context;
        this.mainRecyclerView = mainRecyclerView;
        this.notificationType = notificationType;
        this.progressLayout = progressLayout;
        this.frameLayout = frameLayout;
        this.noAvailableFavesLayout = noAvailableFavesLayout;
        this.noFavesLayout = noFavesLayout;
        this.activity = activity;
    }

    public MenuRetrievalTask(Context context, RecyclerView mainRecyclerView, int notificationType){
        this.context = context;
        this.mainRecyclerView = mainRecyclerView;
        this.notificationType = notificationType;
        this.progressLayout = null;
        this.frameLayout = null;
    }

    @Override
    protected void onPreExecute() {

        super.onPreExecute();

        if(progressLayout != null){
            progressLayout.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            mainRecyclerView.setVisibility(View.GONE);
        }

    }

    @Override
    protected ArrayList<DiningCourtMenu> doInBackground(Void... voids) {

        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        String date = dateFormat.format(new Date());

        ArrayList<DiningCourtMenu> diningCourtMenus = new ArrayList<>();

        try {
            //Synchronous retrofit call
            Response<DiningCourtWrapperModel> locationsResponse = MenuApiHelper.getInstance().getLocations().execute();
            if (locationsResponse.isSuccessful()) {
                for (DiningCourtModel diningCourt : locationsResponse.body().Location) {
                    Response<MenuModel> menuResponse = MenuApiHelper.getInstance().getMenu(diningCourt.Name, date).execute();
                    diningCourtMenus.add(new DiningCourtMenu(diningCourt.Name, menuResponse.body()));
                }
            } else {
                Log.e("Retrofit synch call: ", locationsResponse.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Retrofit synch call: ", e.getMessage());
        }

        return diningCourtMenus;
    }

    @Override
    protected void onPostExecute(ArrayList<DiningCourtMenu> menus) {


        List<FoodModel> faves = SharedPrefsHelper.getFaveList(context);
        ArrayList<DiningCourtMenu> availableFaves = new ArrayList<>();

        for(DiningCourtMenu menu: menus){
            DiningCourtMenu faveCourts = checkForFaves(faves, menu);
            if(faveCourts != null) {
                availableFaves.add(checkForFaves(faves, menu));
            }
        }

        StringBuilder breakfastMessageBuilder = new StringBuilder().append("Faves available at ");
        StringBuilder lunchMessageBuilder = new StringBuilder().append("Faves available at ");
        StringBuilder dinnerMessageBuilder = new StringBuilder().append("Faves available at ");

        boolean breakfastAvailable = false, lunchAvailable = false, dinnerAvailable = false;
        int spaceBrek=0, spaceLunch=0, spaceDinner=0;
        //Make a list of all the foods available today so we can mark foods that aren't in it as unavailable
        ArrayList<FoodModel> availableToday = new ArrayList<>();
        for(DiningCourtMenu menu : availableFaves){
            if(menu != null) {
                if (menu.getBreakfast() != null) {
                    availableToday.addAll(menu.getBreakfast());
                }

                if (menu.getLunch() != null) {
                    availableToday.addAll(menu.getLunch());
                }

                if (menu.getDinner() != null) {
                    availableToday.addAll(menu.getDinner());
                }
            }
        }


        for(FoodModel food : faves){
            if(!availableToday.contains(food)){
                food.setAvailable(false);
            }

            food.setAvailableCourts(new HashMap<>());
        }


        for(DiningCourtMenu menu : availableFaves){
            if(menu != null){

                String courtName = menu.getCourtName();
                if(menu.getBreakfast().size() > 0){

                    //Needed to update availability on the cardViews
                    for(FoodModel foodModel: menu.getBreakfast()){
                        faves.get(faves.indexOf(foodModel)).setAvailable(true);

                        addAvailableCourt("Breakfast", courtName, faves.get(faves.indexOf(foodModel)));
                    }

                    spaceBrek++;
                    breakfastMessageBuilder.append(courtName + ", ");
                    breakfastAvailable = true;
                }

                if(menu.getLunch().size() > 0){

                    for(FoodModel foodModel: menu.getLunch()){
                        faves.get(faves.indexOf(foodModel)).setAvailable(true);

                        addAvailableCourt("Lunch", courtName, faves.get(faves.indexOf(foodModel)));
                    }

                    spaceLunch++;
                    lunchMessageBuilder.append(courtName + ", ");
                    lunchAvailable = true;
                }

                if(menu.getDinner().size() > 0){

                    for(FoodModel foodModel: menu.getDinner()){
                        faves.get(faves.indexOf(foodModel)).setAvailable(true);

                        addAvailableCourt("Dinner", courtName, faves.get(faves.indexOf(foodModel)));
                    }

                    spaceDinner++;
                    dinnerMessageBuilder.append(courtName + ", ");
                    dinnerAvailable = true;
                }


            }

        }


        if(mainRecyclerView != null) {
            FoodAdapter foodAdapter = (FoodAdapter) mainRecyclerView.getAdapter();

            activity.checkForFaves(faves);

            if(SharedPrefsHelper.getSharedPrefs(context).getBoolean("availabilitySwitchChecked", false)) {

                ArrayList<FoodModel> filteredFaves = filterAvailableFaves(new ArrayList<>(faves));
                foodAdapter.setFoods(filteredFaves);
                if(filteredFaves.size() > 0){
                    noAvailableFavesLayout.setVisibility(View.GONE);
                    mainRecyclerView.setVisibility(View.VISIBLE);
                }else{
                    if(SharedPrefsHelper.getFaveList(context).size() > 0) {
                        noAvailableFavesLayout.setVisibility(View.VISIBLE);
                        mainRecyclerView.setVisibility(View.GONE);
                        progressLayout.setVisibility(View.GONE);
                    }
                }
            }else{
                Collections.sort(faves);
                if (!faves.isEmpty() && foodAdapter != null) {
                    foodAdapter.setFoods(faves);
                }
                mainRecyclerView.setVisibility(View.VISIBLE);
            }

            if(foodAdapter != null) {
                foodAdapter.notifyDataSetChanged();
            }
        }

        //Need to call this so when main activity resumes it remembers availability
        SharedPrefsHelper.storeFaveList(faves, context);

        if(breakfastAvailable && notificationType == BREAKFAST_NOTIFICATION){
            if(spaceBrek>0){
                breakfastMessageBuilder.replace(breakfastMessageBuilder.lastIndexOf(","),breakfastMessageBuilder.lastIndexOf(",")+1,"") ;
            }
            if(spaceBrek>1){
                breakfastMessageBuilder.insert(breakfastMessageBuilder.lastIndexOf(",")+1," and") ;
                breakfastMessageBuilder.replace(breakfastMessageBuilder.lastIndexOf(","),breakfastMessageBuilder.lastIndexOf(",")+1,"") ;
            }
            breakfastMessageBuilder.append("for breakfast!");
            NotificationHelper.sendNotification(context, "Faves For Breakfast", breakfastMessageBuilder.toString(), NotificationHelper.BREAKFAST);
        }

        if(lunchAvailable && notificationType == LUNCH_NOTIFICATION){
            if(spaceLunch>0){
                lunchMessageBuilder.replace(lunchMessageBuilder.lastIndexOf(","),lunchMessageBuilder.lastIndexOf(",")+1,"") ;
            }
            if(spaceLunch>1){
                lunchMessageBuilder.insert(lunchMessageBuilder.lastIndexOf(",")+1," and") ;
                lunchMessageBuilder.replace(lunchMessageBuilder.lastIndexOf(","),lunchMessageBuilder.lastIndexOf(",")+1,"") ;
            }
            lunchMessageBuilder.append("for lunch!");
            NotificationHelper.sendNotification(context, "Faves For Lunch", lunchMessageBuilder.toString(), NotificationHelper.LUNCH);
        }

        if(dinnerAvailable && notificationType == DINNER_NOTIFICATION){
            if(spaceDinner>0){
                dinnerMessageBuilder.replace(dinnerMessageBuilder.lastIndexOf(","),dinnerMessageBuilder.lastIndexOf(",")+1,"") ;
            }
            if(spaceDinner>1){
                dinnerMessageBuilder.insert(dinnerMessageBuilder.lastIndexOf(",")+1," and") ;
                dinnerMessageBuilder.replace(dinnerMessageBuilder.lastIndexOf(","),dinnerMessageBuilder.lastIndexOf(",")+1,"") ;
            }
            dinnerMessageBuilder.append("for dinner!");
            NotificationHelper.sendNotification(context, "Faves For Dinner", dinnerMessageBuilder.toString(), NotificationHelper.DINNER);
        }



        //Hide the progress bar
        if(progressLayout != null){
            progressLayout.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            mainRecyclerView.setVisibility(View.VISIBLE);
        }


    }


    //Returns a DiningCourtMenu with only that court's available faves
    //If a court has no available faves, returns null
    private DiningCourtMenu checkForFaves(List<FoodModel> faves, DiningCourtMenu menu){
        ArrayList<FoodModel> breakfast, lunch, dinner;

        breakfast = new ArrayList<>();
        for(FoodModel foodModel: menu.getBreakfast()){
            if(faves.contains(foodModel)){
                breakfast.add(foodModel);
            }
        }

        lunch = new ArrayList<>();
        for(FoodModel foodModel: menu.getLunch()){
            if(faves.contains(foodModel)){
                lunch.add(foodModel);
            }
        }

        dinner = new ArrayList<>();
        for(FoodModel foodModel: menu.getDinner()){
            if(faves.contains(foodModel)){
                dinner.add(foodModel);
            }
        }

        if(breakfast.size() == 0 && lunch.size() == 0 && dinner.size() == 0){
            return null;
        }else{
            return new DiningCourtMenu(menu.getCourtName(), breakfast, lunch, dinner);
        }

    }

    private void addAvailableCourt(String meal, String court, FoodModel foodModel){
        
        HashMap<String, ArrayList<String>> availableCourts = foodModel.getAvailableCourts();
        if(availableCourts == null){
            availableCourts = new HashMap<>();

            availableCourts.put(meal, new ArrayList<>());
            availableCourts.get(meal).add(court);

            foodModel.setAvailableCourts(availableCourts);
        }else{

            if(!availableCourts.containsKey(meal)){
                availableCourts.put(meal, new ArrayList<>());
                availableCourts.get(meal).add(court);
            }else{
                if(!(availableCourts.get(meal).contains(court))){
                    availableCourts.get(meal).add(court);
                }
            }

            foodModel.setAvailableCourts(availableCourts);
        }

    }

    //This should probably not be copied here to avid redundancy but eh (from main activity)
    private ArrayList<FoodModel> filterAvailableFaves(ArrayList<FoodModel> faveList){
        ArrayList<FoodModel> availFaveList = new ArrayList<FoodModel>();

        for(int i = 0; i < faveList.size(); i++) {
            if(faveList.get(i).isAvailable) {
                availFaveList.add(faveList.get(i));
            }
        }

        Collections.sort(availFaveList);

        return availFaveList;
    }
}
