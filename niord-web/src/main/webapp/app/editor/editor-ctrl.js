
/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The Message Editor controller and sub-controllers
 */
angular.module('niord.editor')

    /**
     * Main message editor controller
     */
    .controller('EditorCtrl', ['$scope', '$rootScope', '$stateParams', '$state', '$http', '$timeout', '$uibModal', 'growl',
            'MessageService', 'LangService', 'MapService', 'UploadFileService', 'DateIntervalService',
        function ($scope, $rootScope, $stateParams, $state, $http, $timeout, $uibModal, growl,
                  MessageService, LangService, MapService, UploadFileService, DateIntervalService) {
            'use strict';

            $scope.message = undefined;
            $scope.initId = $stateParams.id || '';
            $scope.referenceType = $stateParams.referenceType || '';

            $scope.editMode = {
                type: false,
                orig_info: false,
                id: false,
                title: false,
                references: false,
                time: false,
                areas: false,
                categories: false,
                positions: false,
                charts: false,
                subject: false,
                description: false,
                attachments: false,
                note: false,
                publication: false,
                source: false,
                prohibition: false,
                signals: false
            };

            $scope.editorFields = $rootScope.editorFieldsBase;
            $scope.unusedEditorFields = {};

            $scope.messageSeries = [];

            $scope.attachmentUploadUrl = undefined;

            // Record if the Edit page was entered from a message list URL
            $scope.backToListUrl = ($rootScope.lastUrl && $rootScope.lastUrl.indexOf('/messages/') != -1)
                        ? $rootScope.lastUrl : undefined;

            // This will be set when the "Save message" button is clicked and will
            // disable the button, to avoid double-clicks
            $scope.messageSaving = false;


            /*****************************/
            /** Initialize the editor   **/
            /*****************************/

            // Used to ensure that description entities have various field
            function initMessageDescField(desc) {
                desc.title = '';
                desc.description = '';
            }

            // Used to ensure that attachment description entities have various field
            function initAttachmentDescField(desc) {
                desc.caption = '';
            }

            // Used to ensure that reference description entities have various field
            function initReferenceDescField(desc) {
                desc.description = '';
            }

            /** Called during initialization when the message has been loaded or instantiated */
            $scope.initMessage = function (pristine) {
                var msg = $scope.message;

                // Sanity check
                if (!msg) {
                    return;
                }

                $scope.setEditorFields(msg.editorFields, true);

                // Ensure that the list of date intervals is defined
                if (!msg.dateIntervals) {
                    msg.dateIntervals = [];
                }

                // Ensure that localized desc fields are defined for all languages
                LangService.checkDescs(msg, initMessageDescField, undefined, $rootScope.modelLanguages);

                // Instantiate the feature collection from the message geometry
                if (!msg.geometry) {
                    msg.geometry = {
                        type: 'FeatureCollection',
                        features: []
                    }
                } else if (!msg.geometry.features) {
                    msg.geometry.features = [];
                }
                $scope.serializeCoordinates();

                // Determine the message series for the current domain and message mainType
                $scope.messageSeries.length = 0;
                if ($rootScope.domain && $rootScope.domain.messageSeries) {
                    angular.forEach($rootScope.domain.messageSeries, function (series) {
                        if (series.mainType == msg.mainType) {
                            if (msg.messageSeries && msg.messageSeries.seriesId == series.seriesId) {
                                $scope.messageSeries.push(msg.messageSeries);
                            } else {
                                $scope.messageSeries.push(series);
                            }
                        }
                    });
                }

                // Ensure that localized reference desc fields are defined for all languages
                if (!msg.references) {
                    msg.references = [];
                }
                angular.forEach(msg.references, function (ref) {
                    LangService.checkDescs(ref, initReferenceDescField, undefined, $rootScope.modelLanguages);
                });

                // Update the attachment upload url
                $scope.attachmentUploadUrl = '/rest/messages/attachments/' + msg.editRepoPath + '/attachments';
                // Ensure that localized attachment desc fields are defined for all languages
                angular.forEach(msg.attachments, function (att) {
                    LangService.checkDescs(att, initAttachmentDescField, undefined, $rootScope.modelLanguages);
                });

                // Mark the form as pristine
                if (pristine) {
                    $scope.setPristine();
                } else {
                    $scope.setDirty();
                }

                // Remove lock on save button
                $scope.messageSaving = false;
            };


            /** Initialize the message editor */
            $scope.init = function () {

                $scope.initId = $stateParams.id || '';
                $scope.referenceType = $stateParams.referenceType || '';
                $scope.editType = 'edit';
                if ($state.includes('editor.template')) {
                    $scope.editType = 'template';
                } else if ($state.includes('editor.copy')) {
                    $scope.editType = 'copy';
                }

                if ($scope.editType == 'edit' && $scope.initId) {
                    // Case 1) The message ID may be specified
                    MessageService.editableDetails($scope.initId)
                        .success(function (message) {
                            $scope.message = message;
                            $scope.initMessage(true);
                        })
                        .error(function (data, status) {
                            growl.error("Error loading message (code: " + status + ")", {ttl: 5000})
                        });

                } else if ($scope.editType == 'copy' && $scope.initId) {
                    // Case 2) Create a copy of a message
                    MessageService.copyMessageTemplate($scope.initId, $scope.referenceType)
                        .success(function (message) {
                            $scope.message = message;
                            $scope.initMessage();
                        })
                        .error(function (data, status) {
                            growl.error("Error copying message (code: " + status + ")", {ttl: 5000})
                        });

                } else if ($scope.editType == 'template') {
                    // Case 3) The editor may be based on a template message, say, from the AtoN selection page
                    $scope.createMessage($rootScope.templateMessage.mainType, $rootScope.templateMessage);
                }
            };


            /*****************************/
            /** Editor functionality    **/
            /*****************************/


            /** Set the form as pristine **/
            $scope.setPristine = function () {
                // $scope.editForm does not work (form tied to sub-scope of this controller?)
                try {
                    angular.element($("#editForm")).scope().editForm.$setPristine();
                } catch (err) {
                }
            };


            /** Set the form as dirty **/
            $scope.setDirty = function () {
                // $scope.editForm does not work (form tied to sub-scope of this controller?)
                try {
                    angular.element($("#editForm")).scope().editForm.$setDirty();
                } catch (err) {
                }
            };


            /** Place focus in the editor **/
            $scope.focusEditor = function () {
                console.log("FOCUS EDITOR");
                $timeout(function () {
                    $('.editor-field-label').first().focus();
                });
            };


            /** Returns if the message is editable **/
            $scope.editable = function () {
                var msg = $scope.message;
                return msg && msg.status &&
                    (msg.status == 'DRAFT' || msg.status == 'VERIFIED' || $rootScope.hasRole('sysadmin'));
            };


            /** Returns if the current user in the current domain can create messages of the given mainType **/
            $scope.canCreateMessage = function (mainType) {
                return $rootScope.supportsMainType(mainType) && $rootScope.hasRole('editor');
            };


            /** Create a message of the given mainType and AtoN selection **/
            $scope.createMessage = function (mainType, templateMessage) {
                if (!$scope.canCreateMessage(mainType)) {
                    return;
                }

                MessageService.newMessageTemplate(mainType)
                    .success(function (message) {
                        $scope.message = message;
                        if (templateMessage) {
                            angular.forEach(templateMessage, function (value, key) {
                                $scope.message[key] = value;
                            });
                        }
                        $scope.initMessage();
                    })
                    .error(function() {
                        growl.error("Error creating new message template", { ttl: 5000 })
                    });
            };


            /** Save the current message **/
            $scope.saveMessage = function () {
                // Perform a couple of validations
                var msg = $scope.message;
                if (!msg.mainType || !msg.type) {
                    growl.error("Please specify message type before saving", { ttl: 5000 });
                    return;
                } else if (!msg.messageSeries) {
                    growl.error("Please specify message series before saving", {ttl: 5000});
                    return;
                }
                if (msg.dateIntervals) {
                    for (var x = 0; x < msg.dateIntervals.length; x++) {
                        var di = msg.dateIntervals[x];
                        if (di.toDate && !di.fromDate) {
                            growl.error("Please specify date interval from-date before saving", {ttl: 5000});
                            return;
                        }
                    }
                }

                // Prevent double-submissions
                $scope.messageSaving = true;

                MessageService.saveMessage($scope.message)
                    .success(function(message) {
                        growl.info("Message saved", { ttl: 3000 });
                        $state.go(
                            'editor.edit',
                            { id: message.id },
                            { reload: true }
                        );
                    })
                    .error(function() {
                        $scope.messageSaving = false;
                        growl.error("Error saving message", { ttl: 5000 })
                    });
            };


            /** Returns if the given message field (group) is valid **/
            $scope.fieldValid = function(fieldId) {
                var msg = $scope.message;
                switch (fieldId) {
                    case 'type':
                        return msg.mainType && msg.type;
                    case 'id':
                        return msg.messageSeries !== undefined;
                }
                return true;
            };


            /** Returns if the given message field (group) should be displayed **/
            $scope.showField = function (fieldId) {
                return $scope.editorFields[fieldId] || $scope.unusedEditorFields[fieldId];
            };


            /** Sets the current editor fields **/
            $scope.setEditorFields = function (editorFields, reset) {
                $scope.editorFields = editorFields || $rootScope.editorFieldsBase;

                // Compute the list of unused editor fields, which may manually be selected
                if (reset) {
                    $scope.unusedEditorFields = {};
                }
                angular.forEach($scope.editorFields, function (enabled, field) {
                    if (enabled) {
                        // Not unused
                        delete $scope.unusedEditorFields[field];
                    } else if (reset || $scope.unusedEditorFields[field] === undefined) {
                        // Unused - don't show
                        $scope.unusedEditorFields[field] = false;
                    }
                })
            };


            /** Toggles whether or not to show the given editor field **/
            $scope.toggleUseEditorField = function (fieldId) {
                $scope.unusedEditorFields[fieldId] = !$scope.unusedEditorFields[fieldId];
            };


            /** Returns the number sequence type of the message series **/
            $scope.numberSequenceType = function () {
                var msg = $scope.message;
                if (msg.messageSeries && $rootScope.domain.messageSeries) {
                    var series = $.grep($rootScope.domain.messageSeries, function (ms) {
                        return ms.seriesId == msg.messageSeries.seriesId;
                    });
                    if (series != null && series.length == 1) {
                        return series[0].numberSequenceType;
                    }
                }
                return null;
            };


            /** Returns if the MRN and short ID is editable **/
            $scope.mrnAndShortIdEditable = function () {
                return $scope.numberSequenceType() == 'MANUAL';
            };


            /** Reference DnD configuration **/
            $scope.referencesSortableCfg = {
                group: 'reference',
                handle: '.move-btn',
                onEnd: $scope.setDirty
            };


            /** Called when a message reference is clicked **/
            $scope.referenceClicked = function(messageId) {
                MessageService.detailsDialog(messageId);
            };


            /** Deletes the given reference from the list of message references **/
            $scope.deleteReference = function (ref) {
                if ($.inArray(ref, $scope.message.references) > -1) {
                    $scope.message.references.splice( $.inArray(ref, $scope.message.references), 1 );
                    $scope.setDirty();
                }
            };


            /** Adds the new reference to the list of message references **/
            $scope.addReference = function () {
                var ref = {
                    messageId: undefined,
                    type: 'REFERENCE',
                    descs: []
                };
                LangService.checkDescs(ref, initReferenceDescField, undefined, $rootScope.modelLanguages);
                $scope.message.references.push(ref);
            };


            /** Deletes the given date interval from the list of message date intervals **/
            $scope.deleteDateInterval = function (dateInterval) {
                if ($.inArray(dateInterval, $scope.message.dateIntervals) > -1) {
                    $scope.message.dateIntervals.splice( $.inArray(dateInterval, $scope.message.dateIntervals), 1 );
                    $scope.setDirty();
                }
            };


            /** Adds a new date interval to the list of message date intervals */
            $scope.addDateInterval = function () {
                $scope.message.dateIntervals.push({ allDay: false, fromDate: undefined, toDate: undefined });
            };


            /** Adds a copy of the given date interval to the list with the given date offset */
            $scope.copyDateInterval = function (dateInterval, offset) {
                var di = angular.copy(dateInterval);
                if (offset && offset > 0) {
                    if (di.fromDate) {
                        di.fromDate = moment(di.fromDate).add(1, "days").valueOf();
                    }
                    if (di.toDate) {
                        di.toDate = moment(di.toDate).add(1, "days").valueOf();
                    }
                }
                $scope.message.dateIntervals.push(di);
            };


            /** Generates textual versions of the current date intervals **/
            $scope.generateTimeDesc = function () {
                angular.forEach($scope.message.descs, function (desc) {
                   desc.time = '';
                    if ($scope.message.dateIntervals.length == 0) {
                        desc.time = DateIntervalService.translateDateInterval(desc.lang, null);
                    } else {
                        angular.forEach($scope.message.dateIntervals, function (di) {
                            desc.time += DateIntervalService.translateDateInterval(desc.lang, di) + '\n';
                        });
                    }
                });
            };


            /** Computes the charts intersecting with the current message geometry **/
            $scope.computeCharts = function () {
                if ($scope.message.geometry.features.length > 0) {
                    MessageService.intersectingCharts($scope.message.geometry)
                        .success(function (charts) {
                            // Prune the returned chart data
                            $scope.message.charts = charts.map(function (chart) {
                                return {
                                    chartNumber: chart.chartNumber,
                                    internationalNumber: chart.internationalNumber,
                                    active: chart.active,
                                    scale: chart.scale
                                }
                            });
                            $scope.setDirty();
                        });
                }
            };


            /** Sorts the current selection of charts **/
            $scope.sortCharts = function () {
                if ($scope.message.charts) {
                    $scope.message.charts.sort(function (c1, c2) {
                        var scale1 = c1.scale || 10000000;
                        var scale2 = c2.scale || 10000000;
                        return scale1 - scale2;
                    });
                }
            };


            /** Copies the locations from the selected area to the message **/
            $scope.copyAreaLocations = function() {
                var msg = $scope.message;
                if (msg.areas) {
                    var areaIds = msg.areas.map(function (a) { return a.id;  }).join(",");
                    $http.get('/rest/areas/search/' + areaIds)
                        .success(function (areas) {
                            angular.forEach(areas, function (area) {
                                if (area.geometry) {
                                    var feature = {
                                        type: 'Feature',
                                        geometry: area.geometry,
                                        properties: { areaId: area.id }
                                    };
                                    angular.forEach(area.descs, function (desc) {
                                        feature.properties['name:' + desc.lang] = desc.name;
                                    });
                                    msg.geometry.features.push(feature);
                                    $scope.geometrySaved(msg.geometry);
                                }
                            });
                        });
                }
            };


            $scope.showCoordinates = false;
            $scope.toggleShowCoordinates = function () {
                $scope.showCoordinates = !$scope.showCoordinates;
            };


            /** Serializes the message coordinates **/
            $scope.featureCoordinates = [];
            $scope.serializeCoordinates = function () {
                // Compute on-demand
                var msg = $scope.message;
                $scope.featureCoordinates.length = 0;
                if (msg.geometry.features.length > 0) {
                    var index = 1;
                    angular.forEach(msg.geometry.features, function (feature) {
                        var coords = [];
                        MapService.serializeCoordinates(feature, coords);
                        if (coords.length > 0) {
                            var name = feature.properties ? feature.properties['name:' + $rootScope.language] : undefined;
                            $scope.featureCoordinates.push({
                                coords: coords,
                                startIndex: index,
                                name: name
                            });
                            index += coords.length;
                        }
                    });
                }
                return $scope.featureCoordinates;
            };

            /** called when the message geometry has been changed **/
            $scope.geometrySaved = function () {
                $scope.serializeCoordinates();
                $scope.setDirty();
            };

            
            /** Returns if the current page matches the given page url **/
            $scope.onPage = function (page) {
                return $state.includes(page);
            };


            /**
             * Called when relevant attributes have been changed that may affect the auto-generated title lines
             * and the editor fields
             */
            $scope.adjustEditableMessage = function () {
                var msg = $scope.message;
                // Construct a message template that contains attributes affecting title line and editor fields
                var msgTemplate = {
                    mainType: msg.mainType,
                    messageSeries: msg.messageSeries,
                    areas: msg.areas,
                    categories: msg.categories,
                    autoTitle: msg.autoTitle,
                    descs: msg.descs.map(function (desc) {
                       return { lang: desc.lang, subject: desc.subject, vicinity: desc.vicinity }
                    })
                };
                MessageService.adjustEditableMessage(msgTemplate)
                    .success(function (message) {

                        $scope.setEditorFields(message.editorFields);

                        if (msg.autoTitle) {
                            angular.forEach(message.descs, function (desc) {
                                var d = LangService.descForLanguage($scope.message, desc.lang);
                                if (d) {
                                    d.title = desc.title;
                                }
                            })
                        }
                    })
            };


            /*****************************/
            /** TinyMCE functions       **/
            /*****************************/

            /** File callback function - called from the TinyMCE image and link dialogs **/
            $scope.fileBrowserCallback = function(field_name, url, type, win) {
                $(".mce-window").hide();
                $("#mce-modal-block").hide();
                $scope.$apply(function() {
                    $scope.attachmentDialog('md').result
                        .then(function (result) {
                            $("#mce-modal-block").show();
                            $(".mce-window").show();
                            var file = "/rest/repo/file/" + $scope.message.editRepoPath + "/attachments/"
                                + encodeURIComponent(result.attachment.fileName);
                            win.document.getElementById(field_name).value = file;
                        });
                });
            };

            // TinyMCE file_browser_callback implementation
            $scope.attachmentDialog = function (size) {
                return $uibModal.open({
                    templateUrl: '/app/editor/message-file-dialog.html',
                    controller: function ($scope, $modalInstance, message) {
                        $scope.message = message;
                        $scope.cancel = function () { $modalInstance.dismiss('cancel'); };
                        $scope.attachmentClicked = function (att) {
                            $modalInstance.close(att);
                        }
                    },
                    size: size,
                    windowClass: 'on-top',
                    resolve: {
                        message: function () { return $scope.message; }
                    }
                });
            };


            /** Dialog that facilitates formatting selected message features as text **/
            $scope.locationsDialog = function (lang) {
                return $uibModal.open({
                    templateUrl: '/app/editor/format-locations-dialog.html',
                    controller: 'FormatMessageLocationsDialogCtrl',
                    size: 'lg',
                    resolve: {
                        featureCollection: function () { return $scope.message.geometry; },
                        lang: function () { return lang; }
                    }
                });
            };


            /** Allows the user to select which location should be inserted and how it should be formatted **/
            $scope.formatLocations = function (editor) {

                // The ID of the parent div has the format "tinymce-<<lang>>"
                var parentDivId = editor.getElement().parentElement.id;
                var lang = parentDivId.split("-")[1];

                $scope.$apply(function() {
                    $scope.locationsDialog(lang).result
                        .then(function (result) {
                            editor.insertContent(result);
                        });
                });
            };


            // Configuration of the TinyMCE editors
            $scope.tinymceOptions = {
                resize: false,
                valid_elements : '*[*]', // NB: This allows insertion of all html elements, including javascript
                // theme: "modern",
                // skin: 'light',
                statusbar : false,
                menubar: false,
                content_css : '/css/messages.css',
                body_class : 'message-description',
                plugins: [ "autolink lists link image anchor", "code fullscreen textcolor", "media table contextmenu paste" ],
                contextmenu: "link image inserttable | cell row column deletetable",
                toolbar: "styleselect | bold italic | forecolor backcolor | alignleft aligncenter alignright alignjustify | "
                         + "bullist numlist  | outdent indent | link image table | code fullscreen | niordlocations",
                table_class_list: [
                    { title: 'None', value: ''},
                    { title: 'No border', value: 'no-border'},
                    { title: 'Condensed', value: 'condensed'},
                    { title: 'No border + condensed', value: 'no-border condensed'}
                ],
                table_cell_class_list: [
                    {title: 'None', value: ''},
                    {title: 'Underline', value: 'underline'}
                ],
                table_row_class_list: [
                    {title: 'None', value: ''},
                    {title: 'Underline', value: 'underline'}
                ],
                file_browser_callback: $scope.fileBrowserCallback,
                setup : function ( editor ) {
                    editor.addButton( 'niordlocations', {
                        title: 'Insert Locations',
                        icon: 'map-marker',
                        onclick : function () { $scope.formatLocations(editor); }
                    });
                }
            };


            /*****************************/
            /** Action menu functions   **/
            /*****************************/


            /** Expands or collapses all field editors **/
            $scope.expandCollapseFields = function (expand) {
                angular.forEach($scope.editMode, function (value, key) {
                    if (value !== expand) {
                        $scope.editMode[key] = expand;
                    }
                });
            };


            /** Opens the message print dialog */
            $scope.pdf = function () {
                if ($scope.message.created) {
                    MessageService.printMessage($scope.message.id);
                }
            };


            /** Opens a dialog that allows the editor to compare this message with another message **/
            $scope.compareMessages = function () {
                if ($scope.message.created) {
                    MessageService.compareMessagesDialog($scope.message.id, undefined);
                }
            };


            /*****************************/
            /** Thumbnails handling     **/
            /*****************************/

            $scope.imgCacheBreaker = new Date().getTime();


            /** Called when the thumbnail has changed **/
            $scope.thumbnailUpdated = function () {
                $scope.imgCacheBreaker = new Date().getTime();
                $scope.setDirty();
            };


            /** Opens the message thumbnail dialog */
            $scope.messageThumbnailDialog = function () {
                $uibModal.open({
                    controller: "MessageThumbnailDialogCtrl",
                    templateUrl: "/app/editor/message-thumbnail-dialog.html",
                    size: 'sm',
                    resolve: {
                        message: function () { return $scope.message; }
                    }
                }).result.then(function (image) {
                    if (image && $scope.message.editRepoPath) {
                        MessageService.changeMessageMapImage($scope.message.editRepoPath, image)
                            .success(function () {
                                $scope.thumbnailUpdated();
                                growl.info("Updated message thumbnail", { ttl: 3000 });
                            });
                    }
                });
            };


            /** Opens the upload-message thumbnail dialog **/
            $scope.uploadMessageThumbnailDialog = function () {
                if ($scope.message.editRepoPath) {
                    UploadFileService.showUploadFileDialog(
                        'Upload thumbnail image',
                        '/rest/message-map-image/' + $scope.message.editRepoPath,
                        'png,jpg,jpeg,gif').result
                        .then($scope.thumbnailUpdated);
                }
            };


            /** Clears the current message thumbnail **/
            $scope.clearMessageThumbnail = function () {
                if ($scope.message.editRepoPath) {
                    MessageService.deleteMessageMapImage($scope.message.editRepoPath)
                        .success(function () {
                            $scope.thumbnailUpdated();
                            growl.info("Deleted message thumbnail", { ttl: 3000 })
                        });
                }
            };


            /*****************************/
            /** Attachments             **/
            /*****************************/


            /** Called when new attachments have successfully been uploaded **/
            $scope.attachmentsUploaded = function (result) {
                if (!$scope.message.attachments) {
                    $scope.message.attachments = [];
                }
                angular.forEach(result, function (attachment) {
                    LangService.checkDescs(attachment, initAttachmentDescField, undefined, $rootScope.modelLanguages);
                    $scope.message.attachments.push(attachment);
                });
                growl.info("Attachments uploaded", { ttl: 3000 });
                $scope.setDirty();
                $scope.$$phase || $scope.$apply();
            };


            /** Called when new attachments have failed to be uploaded **/
            $scope.attachmentsUploadError = function(status, statusText) {
                growl.info("Error uploading attachments: " + statusText, { ttl: 5000 });
                $scope.$$phase || $scope.$apply();
            };


            /** Deletes the given attachment **/
            $scope.deleteAttachment = function (attachment) {
                if ($.inArray(attachment, $scope.message.attachments) > -1) {
                    var filePath = MessageService.attachmentRepoPath($scope.message, attachment);
                    MessageService.deleteAttachmentFile(filePath)
                        .success(function () {
                            $scope.message.attachments.splice( $.inArray(attachment, $scope.message.attachments), 1 );
                            $scope.setDirty();
                        })
                        .error(function () {
                            // NB: We allow deletion of the attachment event if the physical file did not exist
                            $scope.message.attachments.splice( $.inArray(attachment, $scope.message.attachments), 1 );
                            $scope.setDirty();
                        });
                }
            };
            

            /** Returns if the attachment can be displayed inline **/
            $scope.canDisplayAttachment = function (att) {
                return att.type && (att.type.startsWith('image/') || att.type.startsWith('video/'));
            };


            /** Called when the display property is updated **/
            $scope.attachmentDisplayUpdated = function (att) {
                if (att.display == '') {
                    delete att.display;
                    delete att.width;
                    delete att.height;
                }
            };

            /*****************************/
            /** Bootstrap editor        **/
            /*****************************/

            $scope.init();
        }])



    /*******************************************************************
     * EditorCtrl sub-controller that handles message status changes.
     *******************************************************************/
    .controller('EditorStatusCtrl', ['$scope', '$rootScope', '$state', 'growl', 'MessageService', 'DialogService',
        function ($scope, $rootScope, $state, growl, MessageService, DialogService) {
            'use strict';

            $scope.reloadMessage = function (msg) {
                // Call parent controller init() method
                $scope.init();
                if (msg) {
                    growl.info(msg, {ttl: 3000});
                }
            };


            /** Return if the field (e.g. "title") is defined in any of the message descriptors */
            function descFieldDefined(field) {
                for (var x = 0; x < $scope.message.descs.length; x++) {
                    var desc = $scope.message.descs[x];
                    if (desc[field] && desc[field].length > 0) {
                        return true;
                    }
                }
                return false;
            }


            /**
             * Returns if the message is in a state where it could be published.
             * The "status" field is not validated, since this is left to the callee to verify this.
             */
            function canPublish() {
                var msg = $scope.message;
                var error = '';
                if (!msg.type) {
                    error += '<li>Type</li>';
                }
                if (!msg.messageSeries) {
                    error += '<li>Message Series</li>';
                }
                if (!descFieldDefined('subject')) {
                    error += '<li>Subject</li>';
                }
                if (!descFieldDefined('title')) {
                    error += '<li>Title</li>';
                }
                if (error.length > 0) {
                    growl.error("Missing fields:\n<ul>" + error + "</ul>", {ttl: 5000});
                    return false;
                }
                return true;
            }


            /** Verify the draft message **/
            $scope.verify = function () {
                if ($scope.message.status != 'DRAFT') {
                    growl.error("Only draft messages can be verified", {ttl: 5000});
                    return;
                }

                // Validate that relevant message fields are defined
                if (!canPublish()) {
                    return;
                }

                DialogService.showConfirmDialog(
                    "Verify draft?", "Verify draft?")
                    .then(function() {
                        MessageService.updateMessageStatus($scope.message, 'VERIFIED')
                            .success(function() { $scope.reloadMessage("Message verified"); })
                            .error(function(err) {
                                growl.error("Verification failed\n" + err, {ttl: 5000});
                            });
                    });
            };


            /** Publish the message **/
            $scope.publish = function () {

                // First check that the message is valid
                if ($scope.message.status != 'VERIFIED') {
                    growl.error("Only verified draft messages can be published", {ttl: 5000});
                    return;
                }

                // Validate that relevant message fields are defined
                if (!canPublish()) {
                    return;
                }

                DialogService.showConfirmDialog(
                    "Publish Message?", "Publish Message?")
                    .then(function() {
                        MessageService.updateMessageStatus($scope.message, 'PUBLISHED')
                            .success(function() { $scope.reloadMessage("Message published"); })
                            .error(function(err) {
                                growl.error("Publishing failed\n" + err, {ttl: 5000});
                            });
                    });
            };


            /** Delete the draft message **/
            $scope.delete = function () {
                if ($scope.message.status != 'DRAFT' && $scope.message.status != 'VERIFIED') {
                    growl.error("Only draft and verified messages can be deleted", {ttl: 5000});
                    return;
                }

                DialogService.showConfirmDialog(
                    "Delete message?", "Delete message?")
                    .then(function() {
                        MessageService.updateMessageStatus($scope.message, 'DELETED')
                            .success(function() { $scope.reloadMessage("Message deleted"); })
                            .error(function(err) {
                                growl.error("Deletion failed\n" + err, {ttl: 5000});
                            });
                    });
            };


            /** Copy the message **/
            $scope.copy = function () {
                DialogService.showConfirmDialog(
                    "Copy Message?", "Copy Message?")
                    .then(function() {
                        // Navigate to the message editor page
                        $state.go(
                            'editor.copy',
                            { id: $scope.message.id,  referenceType : 'REFERENCE' },
                            { reload: true }
                        );
                    });
            };


            /** Cancel the message **/
            $scope.cancel = function () {
                if ($scope.message.status != 'PUBLISHED') {
                    growl.error("Only published messages can be cancelled", {ttl: 5000});
                    return;
                }

                var modalOptions = {
                    closeButtonText: 'Cancel',
                    actionButtonText: 'Confirm Cancellation',
                    headerText: 'Cancel Message',
                    cancelOptions: { createCancelMessage: true },
                    templateUrl: "cancelMessage.html"
                };

                DialogService.showDialog({}, modalOptions)
                    .then(function () {
                        MessageService.updateMessageStatus($scope.message, 'CANCELLED')
                            .success(function() {
                                if (modalOptions.cancelOptions.createCancelMessage) {
                                    // Navigate to the message editor page
                                    $state.go(
                                        'editor.copy',
                                        { id: $scope.message.id,  referenceType : 'CANCELLATION' },
                                        { reload: true }
                                    );
                                } else {
                                    $scope.reloadMessage("Message cancelled");
                                }
                            })
                            .error(function(err) {
                                growl.error("Cancellation failed\n" + err, {ttl: 5000});
                            });
                    });
            };


        }])



    /*******************************************************************
     * EditorCtrl sub-controller that handles message history.
     *******************************************************************/
    .controller('EditorHistoryCtrl', ['$scope', '$rootScope', '$timeout', 'MessageService',
        function ($scope, $rootScope, $timeout, MessageService) {
            'use strict';

            $scope.messageHistory = [];
            $scope.selectedHistory = [];

            /** Loads the message history **/
            $scope.loadHistory = function () {
                if ($scope.message && $scope.message.id) {
                    MessageService.messageHistory($scope.message.id)
                        .success(function (history) {
                            $scope.messageHistory.length = 0;
                            angular.forEach(history, function (hist) {
                                hist.selected = false;
                                $scope.messageHistory.push(hist);
                            })
                        });
                }
            };

            // Load the message history (after the message has been loaded)
            $timeout($scope.loadHistory, 200);


            /** updates the history selection **/
            $scope.updateSelection = function () {
                $scope.selectedHistory.length = 0;
                angular.forEach($scope.messageHistory, function (hist) {
                    if (hist.selected) {
                        $scope.selectedHistory.unshift(hist);
                    }
                });
            }

        }])


    /*******************************************************************
     * EditorCtrl sub-controller that handles message comments.
     *******************************************************************/
    .controller('EditorCommentsCtrl', ['$scope', '$rootScope', '$timeout', 'MessageService',
        function ($scope, $rootScope, $timeout, MessageService) {
            'use strict';

            $scope.comments = [];
            $scope.comment = undefined;

            // Configuration of the Comment TinyMCE editors
            $scope.commentTinymceOptions = {
                resize: false,
                valid_elements : '*[*]', // NB: This allows insertion of all html elements, including javascript
                statusbar : false,
                menubar: false,
                plugins: [ "autolink lists link image anchor", "code textcolor", "media table contextmenu paste" ],
                contextmenu: "link image inserttable | cell row column deletetable",
                toolbar: "styleselect | bold italic | forecolor backcolor | alignleft aligncenter alignright alignjustify | "
                + "bullist numlist  | outdent indent | link image table"
            };


            /** Loads the message history **/
            $scope.loadComments = function () {
                $scope.comment = undefined;
                if ($scope.message && $scope.message.id) {
                    MessageService.comments($scope.message.id)
                        .success(function (comments) {
                            $scope.comments = comments;

                            // Update the number of unacknowledged comments for the message
                            var unackComments = 0;
                            angular.forEach(comments, function (comment) {
                                if (!comment.acknowledgeDate) {
                                    unackComments++;
                                }
                                $scope.message.unackComments = unackComments;
                            })

                        });
                }
            };

            // Load the message comments (after the message has been loaded)
            $timeout($scope.loadComments, 200);

            /** Selects a new comment for viewing/editing **/
            $scope.selectComment = function (comment) {
                $scope.comment = comment;
            };


            /** Returns if the given comment is selected **/
            $scope.isSelected = function (comment) {
                return comment && $scope.comment && comment.id == $scope.comment.id;
            };


            /** Create a template for a new comment **/
            $scope.newComment = function () {
                $scope.comment = { comment: '' };
            };


            /** Saves the current comment comment **/
            $scope.saveComment = function () {
                if ($scope.comment && $scope.comment.id) {
                    MessageService.updateComment($scope.message.id, $scope.comment)
                        .success($scope.loadComments);
                } else if ($scope.comment) {
                    MessageService.createComment($scope.message.id, $scope.comment)
                        .success($scope.loadComments);
                }
            };


            /** Acknowledges the current comment comment **/
            $scope.acknowledge = function () {
                if ($scope.comment && $scope.comment.id) {
                    MessageService.acknowledgeComment($scope.message.id, $scope.comment)
                        .success($scope.loadComments);
                }
            };


            /** Returns if the current comment can be edited **/
            $scope.canEditComment = function () {
                return $scope.comment &&
                    (!$scope.comment.id || $scope.comment.ownComment || $rootScope.hasRole('sysadmin'));
            };


            /** Cancels editing a comment **/
            $scope.cancel = function () {
                $scope.comment = undefined;
            };
        }]);
