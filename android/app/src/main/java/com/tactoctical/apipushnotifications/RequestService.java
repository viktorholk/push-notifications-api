package com.tactoctical.apipushnotifications;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestService {
    private static RequestService instance;

    private RequestQueue requestQueue;
    private static Context context;

    public static synchronized RequestService getInstance(Context _context){
        if (instance == null){
            instance = new RequestService(_context);
        }
        return instance;
    }

    private RequestService(Context _context){
        context = _context;
    }

    public RequestQueue getRequestQueue(){
        if (requestQueue == null){
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req){
        getRequestQueue().add((req));
    }


}
