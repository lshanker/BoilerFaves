package com.lshan.boilerfaves.Networking;

import android.content.Context;
import android.util.Log;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.lshan.boilerfaves.Models.MenuModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import retrofit2.http.HTTP;

/**
 * Created by lshan on 1/4/2018.
 */

public class MenuRetrievalTask extends AsyncTask<Void, Void, ArrayList<MenuModel>>{

    private static final String API_URL = "https://api.hfs.purdue.edu/menus/v1/locations/";

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ArrayList<MenuModel> doInBackground(Void... voids) {

        String[] diningCourts = {"earhart", "ford", "wiley", "windsor", "hillenbrand"};

        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        String date = dateFormat.format(new Date());

        date = "12-01-2017";

        ArrayList<MenuModel> menus = new ArrayList<>();

        for(String court: diningCourts) {

            try {
                URL url = new URL(API_URL + court + "/" + date);
                HttpURLConnection urlConnection= (HttpURLConnection) url.openConnection();

                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while((line = bufferedReader.readLine()) != null){
                        stringBuilder.append(line);
                    }

                    bufferedReader.close();
                    Gson g = new Gson();
                    MenuModel menu = g.fromJson(stringBuilder.toString(), MenuModel.class);
                    menus.add(menu);

                } finally {
                    urlConnection.disconnect();
                }

            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
            }

        }



        return menus;
    }

    @Override
    protected void onPostExecute(ArrayList<MenuModel> menuModels) {
        System.out.println("here");
    }
}
