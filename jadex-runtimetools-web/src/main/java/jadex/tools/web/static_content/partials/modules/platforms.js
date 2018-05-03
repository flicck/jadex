//<![CDATA[

// common controller definition, discussion here: http://stackoverflow.com/questions/15618213/how-to-reuse-one-controller-for-2-different-views
function jadexDownloadCtrl(path) {
	return function($scope, $http) {
		$scope.url = 'https://download.actoron.com/' + path;
		$http.get('https://download.actoron.com/json/' + path).then(function(response) {
			var distris = [];
			angular.forEach(response.data, function (file, key) {
				if (file.name.lastIndexOf(".txt")!=(file.name.length-4)) {
					if (file.name.indexOf("-standard-")>=0 || file.name.indexOf("-pro-")>=0) {
						file.priority = 1;
					} else if (file.name.indexOf("-minimal-")>=0) {
						file.priority = 2;
					} else if (file.name.indexOf("-android-")>=0) {
						file.priority = 3;
					} else {
						file.priority = 4;
					}
					distris.push(file);
					$scope.distris = distris;
				}
			});
		});
	}
}

var app = angular.module('acdownload', []);
app.controller('OSSRelease', [ '$scope', '$http',
		jadexDownloadCtrl('releases/oss/latest/') ]);
app.controller('OSSNightlies', [ '$scope', '$http',
		jadexDownloadCtrl('nightlies/oss/latest/') ]);
app.controller('ProRelease', [ '$scope', '$http',
		jadexDownloadCtrl('releases/pro/latest/') ]);
app.controller('ProNightlies', [ '$scope', '$http',
		jadexDownloadCtrl('nightlies/pro/latest/') ]);

// From: https://gist.github.com/thomseddon/3511330
app.filter('bytes', function() {
	return function(bytes, precision) {
		if (bytes === 0) {
			return '0 bytes'
		}
		if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
			return '-';
		}

		if (typeof precision === 'undefined') {
			precision = 1;
		}

		var units = [ 'bytes', 'kB', 'MB', 'GB', 'TB', 'PB' ];
		var number = Math.floor(Math.log(bytes) / Math.log(1024));
		var val = (bytes / Math.pow(1024, Math.floor(number)))
				.toFixed(precision);

		return (val.match(/\.0*$/) ? val.substr(0, val.indexOf('.'))
				: val)
				+ ' ' + units[number];
	}
});

// From: http://stackoverflow.com/questions/20131553/angularjs-convert-dates-in-controller
app.filter('mydate', function($filter) {
	return function(input) {
		// ISO Format
		/* if (input == null) {
			return "";
		}
		var _date = $filter('date')(new Date(input),
				'yyyy-MM-dd HH:mm:ss Z');*/
		
		// Locale-based format
		var date=new Date(input);
		_date=date.toLocaleDateString()+' '+date.toLocaleTimeString()+' '+$filter('date')(date,'Z')+' ';
		return _date.toUpperCase();
	};
});

//]]>
