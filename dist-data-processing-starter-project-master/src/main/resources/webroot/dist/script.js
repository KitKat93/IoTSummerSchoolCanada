$(document).ready( createMap );

function createMap() {
	
	var greenIcon = L.icon({
    iconUrl: 'icon2.png',
   

    iconSize:     [73, 93], // size of the icon
    //shadowSize:   [50, 64], // size of the shadow
    iconAnchor:   [45, 4], // point of the icon which will correspond to marker's location
    //shadowAnchor: [4, 62],  // the same for the shadow
    popupAnchor:  [-3, -76] // point from which the popup should open relative to the iconAnchor
	});

    var loc = {'lat': 43.897080, 'lon': -78.865763},
        zoomLevel = 14,
        maxZoom = 15,
        mapID = 'map';

    var map = L.map('map').setView(
        [loc.lat, loc.lon],
        zoomLevel
    );

    L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        'maxZoom': maxZoom, 
        'attribution': 'Map data � OpenStreetMap, under CC BY SA'
    }).addTo(map);
	
	//add realtime updates
    realtime = L.realtime({
        url: 'http://localhost:8080/data',
        //url: 'https://wanderdrone.appspot.com/',
        crossOrigin: true,
        type: 'json'
    }, {
        interval: 4 * 100,
        onEachFeature: function(feature, layer) {
            var content = '<h3>Bus<\/h3>' +
            '<p>Bus: ' + feature.properties.busID + '<\/p>'
            layer.bindPopup(content);
        },
        getFeatureId : function(geojsonobject) { return geojsonobject.properties.busID; }
    }).addTo(map);

	realtime.on('update', function() {
		//console.log("Updating via leaflet realtime");
		//var xmlHttp = new XMLHttpRequest();
		//xmlHttp.open( "GET", "http://localhost:8080/data", false ); // false for synchronous request
		//xmlHttp.send();
		//map.fitBounds(realtime.getBounds(), {maxZoom: 3});
	});
};