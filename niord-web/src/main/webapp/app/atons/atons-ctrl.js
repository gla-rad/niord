
/**
 * The message list controller
 *
 * Whenever the message list filter and map settings are updated, the corresponding request parameters
 * are updated. This allows e.g. for bookmarking of the current state.
 */
angular.module('niord.atons')

    /**
     * Controller used on the AtoN page
     */
    .controller('AtonsCtrl', ['$scope', '$rootScope', '$location', '$http', '$timeout', 'AtonService',
        function ($scope, $rootScope, $location, $http, $timeout, AtonService) {
            'use strict';

            /*****************************/
            /** Scope variables         **/
            /*****************************/

            // AtoN search result
            $scope.atons = [];

            // Max number of AtoNs to fetch in search result
            $scope.maxAtonNo = 1000;

            // Hide filter if we display Selection page
            $scope.showFilter = true;

            // Enlist filter names
            $scope.filterNames = [ 'text', 'chart', 'area' ];

            // Filter state
            $scope.state = {

                /** Map state. Also serves as a mandatory filter in map mode **/
                map : {
                    enabled: true,
                    reloadMap : false // can be used to trigger a map reload
                },

                /** Filter state **/
                text: {
                    enabled: true,
                    focusField: '#query',
                    query: ''
                },
                chart: {
                    enabled: false,
                    focusField: '#charts > div > input.ui-select-search',
                    charts: []
                },
                area: {
                    enabled: false,
                    focusField: '#areas > div > input.ui-select-search',
                    areas: []
                }
            };

            // Selected AtoNs
            $scope.selection = new Map();

            // The GeoJSON FeatureCollection defined by the selected AtoNs
            $scope.featureCollection = {
                type: 'FeatureCollection',
                features: []
            };

            $scope.atonSortableCfg = {
                group: 'aton',
                handle: '.move-btn',
                onEnd: function () {
                    $scope.updateFeatureCollectionCoords();
                }
            };

            /*****************************/
            /** Filter Handling         **/
            /*****************************/


            /** Enables or disables the given filter **/
            $scope.enableFilter = function (name, enabled) {
                var filter = $scope.state[name];
                filter.enabled = enabled;
                if (!enabled) {
                    $scope.clearFilter(name);
                } else if (filter.focusField) {
                    $timeout(function () { $(filter.focusField).focus() });
                }
            };


            /** Clears the given filter **/
            $scope.clearFilter = function (name) {
                var filter = $scope.state[name];
                filter.enabled = false;
                switch (name) {
                    case 'text':
                        filter.query = '';
                        break;
                    case 'chart':
                        filter.charts = [];
                        break;
                    case 'area':
                        filter.areas = [];
                        break;
                }
            };


            /** Clears all filters **/
            $scope.clearAllFilters = function () {
                angular.forEach($scope.filterNames, function (name) {
                    $scope.clearFilter(name);
                })
            };


            /** Converts the filters into a request parameter string **/
            $scope.toRequestFilterParameters = function () {

                var params = '';
                var s = $scope.state;

                // Handle map
                if (s.map.enabled && s.map.extent && s.map.extent.length == 4) {
                    params += '&minLon=' + s.map.extent[0] + '&minLat=' + s.map.extent[1] +
                        '&maxLon=' + s.map.extent[2] + '&maxLat=' + s.map.extent[3];
                }

                // Handle Filters
                if (s.text.enabled) {
                    params += '&name=' + encodeURIComponent(s.text.query);
                }
                if (s.chart.enabled) {
                    angular.forEach(s.chart.charts, function (chart) {
                        params += '&chart=' + chart.chartNumber;
                    })
                }
                if (s.area.enabled) {
                    angular.forEach(s.area.areas, function (area) {
                        params += '&area=' + area.id;
                    })
                }

                params += '&maxAtonNo=' + $scope.maxAtonNo + '&emptyOnOverflow=true';

                // Skip first '&'
                return params.length > 0 ? params.substr(1) : '';
            };


            /** Called when the filter is updated **/
            $scope.filterUpdated = function () {
                $scope.refreshAtons();
            };


            // Use for charts selection
            $scope.charts = [];
            $scope.refreshCharts = function(name) {
                if (!name || name.length == 0) {
                    return [];
                }
                return $http.get(
                    '/rest/charts/search?name=' + encodeURIComponent(name) + '&lang=' + $rootScope.language + '&limit=10'
                ).then(function(response) {
                    $scope.charts = response.data;
                });
            };


            // Use for area selection
            $scope.areas = [];
            $scope.refreshAreas = function(name) {
                if (!name || name.length == 0) {
                    return [];
                }
                return $http.get(
                    '/rest/areas/search?name=' + encodeURIComponent(name)
                        + '&domain=' + ($rootScope.domain !== undefined)
                        + '&lang=' + $rootScope.language
                        + '&limit=10&geometry=true'
                ).then(function(response) {
                    $scope.areas = response.data;
                });
            };

            /** Recursively formats the names of the parent lineage for areas and categories **/
            $scope.formatParents = function(child) {
                var txt = undefined;
                if (child) {
                    txt = (child.descs && child.descs.length > 0) ? child.descs[0].name : 'N/A';
                    if (child.parent) {
                        txt = $scope.formatParents(child.parent) + " - " + txt;
                    }
                }
                return txt;
            };


            // Only apply the map extent as a filter if the map view mode used
            $scope.$watch(
                function () { return $location.path(); },
                function (newValue) {
                    $scope.state.map.enabled = newValue && newValue.endsWith('/map');
                    $scope.showFilter = newValue && !newValue.endsWith('/selected');
                },
                true);


            /*****************************/
            /** AtoN List Handling      **/
            /*****************************/


            /** Monitor changes to the state **/
            $scope.$watch("state", $scope.filterUpdated, true);


            /** Called when the state have been updated **/
            $scope.refreshAtons = function () {
                // TODO: use $timeout and cancel old timeout when new arrives...

                var params = $scope.toRequestFilterParameters();

                AtonService.searchAtonsByParams(params).success(
                    function (result) {
                        $scope.totalAtonNo = result.total;
                        if (result.data != null) {
                            $scope.atons = result.data;
                            // Update the icon urls
                            angular.forEach($scope.atons, function (aton) {
                                aton.iconUrl = AtonService.getAtonIconUrl(aton);
                            });
                        } else {
                            $scope.atons.length = 0;
                        }
                    });
            };


            /*****************************/
            /** Selection handling      **/
            /*****************************/

            /** Returns if the given AtoN is selected or not **/
            $scope.isSelected = function (aton) {
                return $scope.selection.get(AtonService.getAtonUid(aton)) !== undefined;
            };


            /** Clears the AtoN selection **/
            $scope.clearSelection = function () {
                $scope.selection.clear();
            };


            /** Selects all AtoNs **/
            $scope.selectAll = function () {
                angular.forEach($scope.atons, function (aton) {
                    if (!$scope.isSelected(aton)) {
                        // NB: We add a copy that can be modified on the selection page
                        $scope.selection.put(AtonService.getAtonUid(aton), {
                            aton: angular.copy(aton),
                            orig: aton
                        });
                    }
                })
            };


            /** Update the coordinates of the feature collection from the AtoNs **/
            $scope.updateFeatureCollectionCoords = function () {
                // First, remove features with empty AtoN lists
                $scope.featureCollection.features = $.grep($scope.featureCollection.features, function(feature){
                    return feature.properties.atons.length > 0;
                });

                // Next, set validity of features
                angular.forEach($scope.featureCollection.features, function (feature) {
                    var type = feature.geometry.type;
                    var atonNo = feature.properties.atons.length;
                    feature.properties.valid = (type == 'Point' && atonNo == 1) ||
                        (type == 'MultiPoint' && atonNo >= 1) ||
                        (type == 'LineString' && atonNo > 1) ||
                        (type == 'Polygon' && atonNo > 2);
                });

                // Update the coordinates of each feature from the AtoN list
                angular.forEach($scope.featureCollection.features, function (feature) {
                    var aton = feature.properties.atons[0];
                    switch (feature.geometry.type) {
                        case 'Point':
                            feature.geometry.coordinates = [ aton.lon, aton.lat ];
                            break;
                        case 'MultiPoint':
                        case 'LineString':
                            feature.geometry.coordinates = [];
                            angular.forEach(feature.properties.atons, function (aton) {
                                feature.geometry.coordinates.push([aton.lon, aton.lat]);
                            });
                            break;
                        case 'Polygon':
                            feature.geometry.coordinates = [[]];
                            angular.forEach(feature.properties.atons, function (aton) {
                                feature.geometry.coordinates[0].push([aton.lon, aton.lat]);
                            });
                            feature.geometry.coordinates[0].push([aton.lon, aton.lat]);
                            break;
                    }
                });
            };

            /** Updates the FeatureCollection based on the currently selected AtoNs **/
            $scope.updateFeatureCollection = function () {

                // Remove AtoNs from the features that is no longer present in the selection
                var featureAtonUids = {};
                angular.forEach($scope.featureCollection.features, function (feature) {
                    feature.properties.atons = $.grep(feature.properties.atons, function(aton) {
                        return $scope.isSelected(aton);
                    });
                    angular.forEach(feature.properties.atons, function (aton) {
                        featureAtonUids[AtonService.getAtonUid(aton)] = true;
                    });
                });

                // Determine newly selected AtoNs
                var newAtonUids = $.grep($scope.selection.keys, function (key) {
                    return featureAtonUids[key] == null;
                });

                // Add features for the new AtoNs
                angular.forEach(newAtonUids, function (atonUid) {
                    var feature = {
                        type: 'Feature',
                        properties: {
                            atons: [ $scope.selection.get(atonUid).aton ]
                        },
                        geometry: {
                            type: 'Point',
                            coordinates: []
                        }
                    };
                    $scope.featureCollection.features.push(feature);
                });

                // Update the feature coordinates
                $scope.updateFeatureCollectionCoords();
            };

            $scope.$watchCollection("selection.keys", $scope.updateFeatureCollection, true);

            $scope.$on('aton-updated', function (event, aton) {
                if ($scope.isSelected(aton)) {
                    $scope.updateFeatureCollection();
                }
            });

            /** Utility function that swaps two elements of an array **/
            function swapElements(arr, index1, index2) {
                if (index1 >= 0 && index1 < arr.length &&
                    index2 >= 0 && index2 < arr.length) {
                    var tmp = arr[index1];
                    arr[index1] = arr[index2];
                    arr[index2] = tmp;
                }
            }


            /** Deletes the given feature from the FeatureCollection **/
            $scope.deleteFeature = function (feature) {
                angular.forEach(feature.properties.atons, function (aton) {
                    $scope.selection.remove(AtonService.getAtonUid(aton));
                });
            };


            /** Decrease the index of the feature in the FeatureCollection **/
            $scope.moveFeatureUp = function (feature) {
                var index = $.inArray(feature, $scope.featureCollection.features);
                if (index != -1) {
                    swapElements($scope.featureCollection.features, index, index - 1);
                }
            };

            /** Increase the index of the feature in the FeatureCollection **/
            $scope.moveFeatureDown = function (feature) {
                var index = $.inArray(feature, $scope.featureCollection.features);
                if (index != -1) {
                    swapElements($scope.featureCollection.features, index, index + 1);
                }
            };

            /*****************************/
            /** Utility functions       **/
            /*****************************/


            /** Returns the bottom-left point of the current map extent **/
            $scope.extentBottomLeft = function () {
                var extent = $scope.state.map.extent;
                return extent
                    ? { lon: extent[0], lat: extent[1] }
                    : '';
            };


            /** Returns the top-right point of the current map extent **/
            $scope.extentTopRight = function () {
                var extent = $scope.state.map.extent;
                return extent
                    ? { lon: extent[2], lat: extent[3] }
                    : '';
            };

        }])



    /**
     * Dialog Controller used for editing the details of an AtoN.
     *
     * Important: Only the local AtoN instance is updated. The
     * AtoN is not persisted to the database.
     */
    .controller('EditAtonDetailsDialogCtrl', ['$scope', '$rootScope', '$timeout', 'AtonService', 'atonCtx', 'editable',
        function ($scope, $rootScope, $timeout, AtonService, atonCtx, editable) {
            'use strict';

            $scope.editable = editable;
            $scope.aton = angular.copy(atonCtx.aton);
            $scope.tags = [];
            $scope.newTag = { k: '', v: '' };

            var loadTimer;


            /** Loads the SVG icon for the current AtoN **/
            $scope.updateSvgImage = function () {
                loadTimer = undefined;
                AtonService.getAtonSvg($scope.aton)
                    .success(function (svg) {
                        $('#aton-details-image').html(svg);
                    });
            };
            $scope.updateSvgImage();


            /** Loads the SVG icon for the current AtoN via a timer **/
            $scope.timedUpdateSvgImage = function () {
                if (loadTimer) {
                    $timeout.cancel(loadTimer);
                }
                if ($scope.atonDetailsForm) {
                    $scope.atonDetailsForm.$setDirty();
                }
                $scope.aton.iconUrl = AtonService.getAtonIconUrl($scope.aton);
                loadTimer = $timeout($scope.updateSvgImage, 500);
            };


            /** Called whenever the AtoN has been updated **/
            $scope.atonUpdated = function () {

                $scope.tags.length = 0;
                angular.forEach($scope.aton.tags, function (v, k) {
                    $scope.tags.push({ k: k, v: v });
                });
                // Sort by tag key
                $scope.tags.sort(function(t1, t2){
                    return ((t1.k < t2.k) ? -1 : ((t1.k > t2.k) ? 1 : 0));
                });

                // Update the SVG
                $scope.timedUpdateSvgImage();
            };
            $scope.atonUpdated();


            /** Called whenever the AtoN tags array has been updated **/
            $scope.tagsUpdated = function () {

                $scope.aton.tags = {};
                angular.forEach($scope.tags, function (tag) {
                    $scope.aton.tags[tag.k] = tag.v;
                });

                // Update the SVG
                $scope.timedUpdateSvgImage();
            };
            $scope.$watch("tags", $scope.tagsUpdated, true);


            /** Destroy any pending SVG-update timers **/
            $scope.$on('$destroy', function() {
                if (angular.isDefined(loadTimer)) {
                    $timeout.cancel(loadTimer);
                    loadTimer = undefined;
                }
            });


            /** Deletes the AtoN tag with the given index */
            $scope.deleteAtonTag = function (index) {
                $scope.tags.splice(index, 1);
            };


            /** Adds the new AtoN tag to the AtoN */
            $scope.addNewAtonTag = function () {
                var k = $scope.newTag.k;
                var v = $scope.newTag.v;
                if (k.length > 0 && v.length) {
                    $scope.aton.tags[k] = v;
                    $scope.atonUpdated();
                }
                $scope.newTag = { k: '', v: '' };
            };


            /** Creates an auto-complete node type list */
            $scope.nodeTypeNames = [];
            $scope.selcetedNodeTypes = { names: [] };
            $scope.refreshNodeTypeNames = function (name) {
                return AtonService.getNodeTypeNames(name).then(function(response) {
                    $scope.nodeTypeNames = response.data;
                });
            };


            /** Merges the current AtoN definition with the node types currently selected */
            $scope.mergeWithNodeType = function () {
                if ($scope.selcetedNodeTypes.names.length == 0) {
                    return;
                }
                var names = angular.copy($scope.selcetedNodeTypes.names);
                AtonService.mergeWithNodeTypes($scope.aton, names).then(function(response) {
                    $scope.aton.tags = response.data.tags;
                    $scope.atonUpdated();
                });
                $scope.nodeTypeNames.length = 0;
                $scope.selcetedNodeTypes.names.length = 0;
            };


            /** Returns if the given attribute is changed compared with the original */
            $scope.changed = function (attr) {
                if (attr) {
                    return !angular.equals($scope.aton[attr], atonCtx.orig[attr]);
                }
                // If no attribute is specified, check if there are any changes
                return !angular.equals($scope.aton, atonCtx.orig);
            };


            /** Revert all AtoN changes */
            $scope.revert = function () {
                angular.copy(atonCtx.orig, $scope.aton);
                $scope.atonUpdated();
                $scope.atonDetailsForm.$setDirty();
            };


            /** Closes the dialog and update the original AtoN */
            $scope.save = function () {
                // Copy the changes to the original AtoN
                angular.copy($scope.aton, atonCtx.aton);

                $scope.$close($scope.aton);

                // Broadcast the change
                $rootScope.$broadcast('aton-updated', $scope.aton);
            }
        }]);

