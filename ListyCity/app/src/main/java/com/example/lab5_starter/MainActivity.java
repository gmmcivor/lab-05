package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // Initialize city list and adapter
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Listen for live Firestore updates
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Listen failed: ", error);
                return;
            }
            if (value != null) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // Add new city button
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // Click = view/edit details
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });

        // Long click = delete city
        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City cityToDelete = cityArrayAdapter.getItem(i);
            if (cityToDelete != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete City")
                        .setMessage("Are you sure you want to delete " + cityToDelete.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteCity(cityToDelete))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            return true;
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // Update Firestore by overwriting the document
        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City updated: " + city.getName()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating city", e));
    }

    @Override
    public void addCity(City city) {
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        // Add new city to Firestore
        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City added: " + city.getName()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding city", e));
    }

    private void deleteCity(City city) {
        // Remove locally
        cityArrayList.remove(city);
        cityArrayAdapter.notifyDataSetChanged();

        // Remove from Firestore
        citiesRef.document(city.getName())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City deleted: " + city.getName()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting city", e));
    }
}
