package com.droidhats.campuscompass.views

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Html.fromHtml
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.droidhats.campuscompass.MainActivity
import com.droidhats.campuscompass.models.Building
import com.droidhats.campuscompass.models.CalendarEvent
import com.droidhats.campuscompass.R
import com.droidhats.campuscompass.viewmodels.MapViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.android.PolyUtil
import com.mancj.materialsearchbar.MaterialSearchBar
import java.io.IOException
import java.util.Locale
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.listOf
import kotlin.collections.MutableList
import kotlinx.android.synthetic.main.bottom_sheet_layout.bottom_sheet
import kotlinx.android.synthetic.main.bottom_sheet_layout.radioTransportGroup
import kotlinx.android.synthetic.main.map_fragment.buttonInstructions
import kotlinx.android.synthetic.main.map_fragment.searchBar
import kotlinx.android.synthetic.main.map_fragment.toggleButton
import org.json.JSONArray
import org.json.JSONObject
import com.droidhats.campuscompass.models.Map

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolygonClickListener, CalendarFragment.OnCalendarEventClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val AUTOCOMPLETE_REQUEST_CODE = 3
        private const val MAP_PADDING_TOP = 200
        private const val MAP_PADDING_RIGHT = 15
        var stepInsts : String = ""
    }

    private var instructions = arrayListOf<String>()
    private var stepInstructions: String = ""
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var viewModel: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.map_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)


        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity as Activity)

        //update lastLocation with the new location and update the map with the new location coordinates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
            }
        }

        //Use this Fragment's implemented calendar event click callback
        CalendarFragment.onCalendarEventClickListener = this

        createLocationRequest()
        initPlacesSearch()
        initBottomSheetBehavior()
        initSearchBar()
        handleCampusSwitch()
        instructionsButton()
    }

    /**
     * Initializes the map and adds markers or lines and attaches listeners
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * @param googleMap a necessary google map object on which we add markers and attach listeners.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        // Get the map from the viewModel.
        map = viewModel.getMap(googleMap, this, this, this.activity as MainActivity)

        //Gives you the most recent location currently available.
        fusedLocationClient.lastLocation.addOnSuccessListener(activity as Activity) { location ->
            // Got last known location. In some rare situations this can be null.
            // If  able to retrieve the the most recent location, then move the camera to the user’s current location.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }

        map.setOnMapClickListener {

            //Dismiss the bottom sheet when clicking anywhere on the map
            dismissBottomSheet()
        }
    }

    private fun createLocationRequest() {
        // 1  create an instance of LocationRequest, add it to an instance of LocationSettingsRequest.Builder and retrieve and handle any changes to be made based on the current state of the user’s location settings.
        locationRequest = LocationRequest()
        // 2   specifies the rate at which your app will like to receive updates.
        locationRequest.interval = 10000
        // 3 specifies the fastest rate at which the app can handle updates. Setting the fastestInterval rate places a limit on how fast updates will be sent to your app.
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        // 4 check location settings before asking for location updates
        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())

        // 5 A task success means all is well and you can go ahead and initiate a location request
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            // 6  A task failure means the location settings have some issues which can be fixed. This could be as a result of the user’s location settings turned off
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        activity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    //get real time updates of current location
    private fun startLocationUpdates() {
        //requests for location updates.
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null //* Looper *//*
        )
    }

    // 1 Override AppCompatActivity’s onActivityResult() method and start the update request if it has a RESULT_OK result for a REQUEST_CHECK_SETTINGS request.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                if (place != null) {
                    Log.i(TAG,"Place: " + place.name + ", " + place.id + ", " + place.latLng)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng, 16.0f))
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                var status = data?.let { Autocomplete.getStatusFromIntent(it) }
            } else if (resultCode == RESULT_CANCELED) {
                // TODO: Handle user cancelling the operation.
            }
        }
    }

    // 2 Override onPause() to stop location update request
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 3 Override onResume() to restart the location update request.
    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    //implements methods of interface GoogleMap.GoogleMap.OnPolygonClickListener
    override fun onPolygonClick(p: Polygon) {
        // Expand the bottom sheet when clicking on a polygon
        // TODO: Limit only to campus buildings as polygons could highlight anything
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }

        // Populate the bottom sheet with building information
        populateAdditionalInfoBottomSheet(p)

        //Navigation here
        val directionsButton: Button = requireActivity().findViewById(R.id.bottom_sheet_directions_button)
        directionsButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            //Get the building object from the polygon that the user clicked on
            var selectedBuilding : Building? = viewModel.findBuildingByPolygonTag(p.tag.toString())


            //TODO: This full clear and redraw should probably be removed when the directions
            // system is implemented. It was added to show only one route at a time
            map.clear()
            drawBuildingPolygons()
            if (selectedBuilding != null) {
                placeMarkerOnMap(LatLng(selectedBuilding.getLocation().latitude, selectedBuilding.getLocation().longitude))
            }

            //Generate directions from current location to the selected building
            fusedLocationClient.lastLocation.addOnSuccessListener(activity as Activity) { location ->
                if (location != null) {

                    if (transportationMode() == "shuttle") {
                        //Setting the top bar "from" to the name of the selected building.

                        // TODO: In the future check selectedBuilding.getName() == SGW_buildings <-- Grab this part from campus.
                        if (selectedBuilding != null) {
                            if (selectedBuilding.getName() == "Henry F. Hall Building" || selectedBuilding.getName() == "EV Building" || selectedBuilding.getName() == "John Molson School of Business" || selectedBuilding.getName() == "Faubourg Saint-Catherine Building" || selectedBuilding.getName() == "Guy-De Maisonneuve Building" || selectedBuilding.getName() == "Faubourg Building" || selectedBuilding.getName() == "Visual Arts Building" || selectedBuilding.getName() == "Pavillion J.W. McConnell Building") { //<-- TO FIX
                                generateDirections(location, selectedBuilding.getLocation(), "shuttleToSGW")
                            } else {
                                generateDirections(location, selectedBuilding.getLocation(), "shuttleToLOY")
                            }
                        }
                    } else {
                        if (selectedBuilding != null) {
                            generateDirections(location, selectedBuilding.getLocation(), transportationMode())
                        }
                    }
                }

                if (transportationMode()!= "shuttle") {
                    //Move the camera to the starting location
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude), 16.0f))
                }

                buttonInstructions.visibility = View.VISIBLE

                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun transportationMode() : String {

        //Checking which transportation mode is selected, default is walking.
        var transportationMode = "driving"
        when (radioTransportGroup.checkedRadioButtonId) {
            R.id.drivingId -> {
                transportationMode = "driving"
            }
            R.id.transitId -> {
                transportationMode = "transit"
            }
            R.id.walkingId -> {
                transportationMode = "walking"
            }
            R.id.bicyclingId -> {
                transportationMode = "bicycling"
            }
            R.id.shuttleId -> {
                transportationMode = "shuttle"
            }
        }
        return transportationMode
    }

    //implements methods of interface   GoogleMap.OnMarkerClickListener
    override fun onMarkerClick(p0: Marker?) = false

    //the Android Maps API lets you use a marker object, which is an icon that can be placed at a particular point on the map’s surface.
    private fun placeMarkerOnMap(location: LatLng) {
        // 1 Create a MarkerOptions object and sets the user’s current location as the position for the marker
        val markerOptions = MarkerOptions().position(location)

        //added a call to getAddress() and added this address as the marker title.
        val titleStr = getAddress(location)
        markerOptions.title(titleStr)

        // 2 Add the marker to the map
        map.addMarker(markerOptions)
    }

    private fun getAddress(latLng: LatLng): String {
        // 1 Creates a Geocoder object to turn a latitude and longitude coordinate into an address and vice versa
        val geocoder = Geocoder(activity as Activity)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // 2 Asks the geocoder to get the address from the location passed to the method
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3 If the response contains any address, then append it to a string and return
            if (null != addresses && addresses.isNotEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        } catch (e: IOException) {
            Log.e("MapFragment", e.localizedMessage!!)
        }

        return addressText
    }

    private fun initPlacesSearch() {
        Places.initialize(activity as Activity, getString(R.string.ApiKey), Locale.CANADA)
        Places.createClient(activity as Activity)
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        searchBar.setOnClickListener {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(activity as Activity)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }
    }

    //Handle the switching views between the two campuses. Should probably move from here later
    private fun handleCampusSwitch() {
        var campusView: LatLng

        //Setting Toggle button listener
        toggleButton.setOnCheckedChangeListener { _, onSwitch ->
            if (onSwitch) {
                campusView = LatLng(45.495637, -73.578235)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(campusView, 17.5f))
            } else {
                campusView = LatLng(45.458159, -73.640450)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(campusView, 17.5f))
            }
            dismissBottomSheet()
        }
    }

    //Handle the clicking of the instructions button. Should probably move from here later
    private fun instructionsButton() {
        //instruction button listener
        buttonInstructions.setOnClickListener {
            for (item in instructions) {
                stepInstructions += item
            }
            stepInsts = fromHtml(stepInstructions).toString()
            instructions.clear() // Array is cleared
            stepInstructions = "" // String instruction cleared
            findNavController().navigate(R.id.action_map_fragment_to_instructionFragment)
        }
    }

    private fun drawBuildingPolygons() {
        //Highlight both SGW and Loyola Campuses
        for (campus in viewModel.getCampuses()) {
            for (building in campus.getBuildings()) {
                map.addPolygon(building.getPolygonOptions()).tag = building.getName()
                val polygon = map.addPolygon(building.getPolygonOptions())
                building.setPolygon(polygon)
            }
        }
    }

    private fun initSearchBar() {
        searchBar.setOnSearchActionListener(object : MaterialSearchBar.OnSearchActionListener{

            override fun onButtonClicked(buttonCode: Int) {
                when(buttonCode) {
                    //Open the Nav Bar
                    MaterialSearchBar.BUTTON_NAVIGATION -> requireActivity().
                        findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(GravityCompat.START)
                }
            }
            override fun onSearchStateChanged(enabled: Boolean) {
            }
            override fun onSearchConfirmed(text: CharSequence?) {
            }
        })
    }

    private fun initBottomSheetBehavior() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // React to state change
                // The following code can be used if we want to do certain actions related
                // to the change of state of the bottom sheet
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Adjusting the google zoom buttons to stay on top of the bottom sheet
                //Multiply the bottom sheet height by the offset to get the effect of them being anchored to the top of the sheet
                map.setPadding(0, MAP_PADDING_TOP, MAP_PADDING_RIGHT, (slideOffset * bottom_sheet.height).toInt())
            }
        })
    }

    private fun dismissBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED || bottomSheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun populateAdditionalInfoBottomSheet(p: Polygon) {
        // Populate the bottom sheet with building information
        val buildingName: TextView = requireActivity().findViewById(R.id.bottom_sheet_building_name)
        val buildingAddress: TextView =
            requireActivity().findViewById(R.id.bottom_sheet_building_address)
        val buildingOpenHours: TextView = requireActivity().findViewById(R.id.bottom_sheet_open_hours)
        val buildingServices: TextView = requireActivity().findViewById(R.id.bottom_sheet_services)
        val buildingDepartments: TextView =
            requireActivity().findViewById(R.id.bottom_sheet_departments)
        val buildingImage: ImageView = requireActivity().findViewById(R.id.building_image)

        for (campus in viewModel.getCampuses()) {
            for (building in campus.getBuildings()) {
                if (building.getPolygon().tag == p.tag) {
                    buildingName.text = p.tag.toString()
                    buildingAddress.text = building.getAddress()
                    buildingOpenHours.text = building.getOpenHours()
                    buildingServices.text = building.getServices()
                    buildingDepartments.text = building.getDepartments()

                    when(building.getPolygon().tag){
                        "Henry F. Hall Building" -> buildingImage.setImageResource(R.drawable.building_hall)
                        "EV Building" -> buildingImage.setImageResource(R.drawable.building_ev)
                        "John Molson School of Business" -> buildingImage.setImageResource(R.drawable.building_jmsb)
                        "Faubourg Saint-Catherine Building" -> buildingImage.setImageResource(R.drawable.building_fg_sc)
                        "Guy-De Maisonneuve Building" -> buildingImage.setImageResource(R.drawable.building_gm)
                        "Faubourg Building" -> buildingImage.setImageResource(R.drawable.building_fg)
                        "Visual Arts Building" -> buildingImage.setImageResource(R.drawable.building_va)
                        "Pavillion J.W. McConnell Building" -> buildingImage.setImageResource(R.drawable.building_webster_library)
                        "Psychology Building" -> buildingImage.setImageResource(R.drawable.building_p)
                        "Richard J. Renaud Science Complex" -> buildingImage.setImageResource(R.drawable.building_rjrsc)
                        "Central Building" -> buildingImage.setImageResource(R.drawable.building_cb)
                        "Communication Studies and Journalism Building" -> buildingImage.setImageResource(R.drawable.building_csj)
                        "Administration Building" -> buildingImage.setImageResource(R.drawable.building_a)
                        "Loyola Jesuit and Conference Centre" -> buildingImage.setImageResource(R.drawable.building_ljacc)
                        else -> Log.v("Error loading images", "couldn't load image")
                    }
                    //TODO: Leaving events empty for now as the data is not loaded from json. Need to figure out in future how to implement
                }
            }
        }
    }

    private fun generateDirections(origin: Location, destination: LatLng, mode: String) {

        val directionsURL:String = when (mode) {
            "shuttleToSGW" -> {
                "https://maps.googleapis.com/maps/api/directions/json?origin=45.497132,-73.578519&destination=45.458398,-73.638241&waypoints=via:45.492767,-73.582678|via:45.463749,-73.628861&mode=" + mode + "&key=" + getString(R.string.ApiKey)
            }
            "shuttleToLOY" -> {
                "https://maps.googleapis.com/maps/api/directions/json?origin=45.458398,-73.638241&destination=45.497132,-73.578519&mode=" + mode + "&key=" + getString(R.string.ApiKey)
            }
            else -> {
                "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin.latitude.toString() + "," + origin.longitude.toString() + "&destination=" + destination.latitude.toString() + "," + destination.longitude.toString() +"&mode=" + mode +"&key=" + getString(R.string.ApiKey)
            }
        }

        if (mode == "shuttleToSGW") {
            //Move the camera to the LOY Campus
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(45.458398,-73.638241), 16.0f))
        }

        //Move the camera to the SGW Campus
        if (mode == "shuttleToLOY") {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(45.497132,-73.578519), 16.0f))
        }

        //Creating the HTTP request with the directions URL
        val directionsRequest = object : StringRequest(
            Method.GET,
            directionsURL,
            Response.Listener { response ->

                //Retrieve response (a JSON object)
                val jsonResponse = JSONObject(response)

                // Get route information from json response
                val routesArray = jsonResponse.getJSONArray("routes")
                val routes = routesArray.getJSONObject(0)
                val legsArray: JSONArray = routes.getJSONArray("legs")
                val legs = legsArray.getJSONObject(0)
                val stepsArray = legs.getJSONArray("steps")

                //Debug
                for (x in 1..3) {
                    Toast.makeText(activity, "The selected Transportation mode is: $mode. Total distance is: ${legs.getJSONObject("distance").getString("text")}. Total travel time is: ${legs.getJSONObject("duration").getString("text")}", Toast.LENGTH_LONG).show()
                }

                val path: MutableList<List<LatLng>> = ArrayList()

                //Build the path polyline as well as store instruction between 2 path into an array.
                for (i in 0 until stepsArray.length()) {
                    val points = stepsArray.getJSONObject(i).getJSONObject("polyline").getString("points")

                    if (mode == "transit" || mode == "walking") {
                        instructions.add(stepsArray.getJSONObject(i).getString("html_instructions") + "<br>Distance: " + stepsArray.getJSONObject(i).getJSONObject("distance").getString("text") + "<br>Duration: " + stepsArray.getJSONObject(i).getJSONObject("duration").getString("text") + "<br>")
                        if (stepsArray.getJSONObject(i).has("steps")) {
                            instructions.add("Instructions:<br>")
                            for (j in 0 until stepsArray.getJSONObject(i).getJSONArray("steps").length()) {instructions.add(stepsArray.getJSONObject(i).getJSONArray("steps").getJSONObject(j).getString("html_instructions") + "<br>")
                            }
                            instructions.add("<br>")
                        }
                        if (stepsArray.getJSONObject(i).has("transit_details")) {
                            instructions.add("Information:<br>")
                            instructions.add("Departure Stop: " + stepsArray.getJSONObject(i).getJSONObject("transit_details").getJSONObject("departure_stop").getString("name") + "<br>")
                            instructions.add("Arrival Stop: " + stepsArray.getJSONObject(i).getJSONObject("transit_details").getJSONObject("arrival_stop").getString("name") + "<br>")
                            instructions.add("Total Number of Stop: " + stepsArray.getJSONObject(i).getJSONObject("transit_details").getString("num_stops") + "<br><br>")
                        }
                    } else {
                        instructions.add(stepsArray.getJSONObject(i).getString("html_instructions") + "<br>")
                    }
                    path.add(PolyUtil.decode(points))
                }

                //Draw the path polyline
                for (i in 0 until path.size) {
                    this.map.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
                }
            },
            Response.ErrorListener {
                Log.e("Volley Error:", "HTTP response error")
            }) {}

        //Confirm and add the request with Volley
        val requestQueue = Volley.newRequestQueue(activity)
        requestQueue.add(directionsRequest)
    }

    override fun onCalendarEventClick(item: CalendarEvent?) {
        findNavController().navigateUp()
        Toast.makeText(context, "Start Navigation for ${item!!.title}", Toast.LENGTH_LONG).show()
    }
}
