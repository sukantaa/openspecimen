
angular.module('os.biospecimen.cp', 
  [
    'ui.router',
    'os.biospecimen.cp.list',
    'os.biospecimen.cp.addedit',
    'os.biospecimen.cp.import',
    'os.biospecimen.cp.detail',
    'os.biospecimen.cp.consents',
    'os.biospecimen.cp.events',
    'os.biospecimen.cp.specimens',
    'os.biospecimen.cp.catalog',
    'os.biospecimen.cp.dp'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('cps', {
        url: '/cps',
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope, cpsCtx) {
          $scope.cpsCtx = cpsCtx;

          // Collection Protocol Authorization Options
          $scope.cpResource = {
            createOpts: {resource: 'CollectionProtocol', operations: ['Create']},
            updateOpts: {resource: 'CollectionProtocol', operations: ['Update']},
            deleteOpts: {resource: 'CollectionProtocol', operations: ['Delete']}
          }
          
          $scope.participantResource = {
            createOpts: {resource: 'ParticipantPhi', operations: ['Create']},
            updateOpts: {resource: 'ParticipantPhi', operations: ['Update']}
          }
          
          $scope.specimenResource = {
            updateOpts: {resource: 'VisitAndSpecimen', operations: ['Create', 'Update']}
          }
          
          $scope.codingEnabled = $scope.global.appProps.cp_coding_enabled;
        },
        resolve: {
          cpsCtx: function(currentUser, AuthorizationService) {
            var participantEximAllowed = AuthorizationService.isAllowed({
              resource: 'ParticipantPhi',
              operations: ['Export Import']
            });

            var visitSpmnEximAllowed = AuthorizationService.isAllowed({
              resource: 'VisitAndSpecimen',
              operations: ['Export Import']
            });

            return {
              participantImportAllowed: participantEximAllowed,
              visitSpecimenImportAllowed: visitSpmnEximAllowed,
              participantExportAllowed: participantEximAllowed,
              visitSpecimenExportAllowed: visitSpmnEximAllowed
            }
          }
        },
        parent: 'signed-in'
      })
      .state('cp-list', {
        url: '?filters', 
        templateUrl: 'modules/biospecimen/cp/list.html',
        controller: 'CpListCtrl',
        parent: 'cps',
        resolve: {
          cpList: function($stateParams, CollectionProtocol, ListPagerOpts, Util) {
            var filterOpts = Util.filterOpts({maxResults: ListPagerOpts.MAX_PAGE_RECS + 1}, $stateParams.filters);
            return CollectionProtocol.list(filterOpts);
          },
          
          view: function($rootScope, $state, cpList) {
            if ($rootScope.stateChangeInfo.fromState.name == 'login') {
              if (cpList.length == 1) {
                $state.go('participant-list', {cpId: cpList[0].id});
              } else if (cpList.length == 0) {
                $state.go('home');
              }
            }
          }
        }
      })
      .state('cp-addedit', {
        url: '/addedit/:cpId?mode',
        templateUrl: 'modules/biospecimen/cp/addedit.html',
        resolve: {
          cp: function($stateParams, CollectionProtocol) {
            if ($stateParams.cpId) {
              return CollectionProtocol.getById($stateParams.cpId);
            }
            return new CollectionProtocol();
          },
          extensionCtxt: function(CollectionProtocol) {
            return CollectionProtocol.getExtensionCtxt();
          }
        },
        controller: 'CpAddEditCtrl',
        parent: 'cps'
      })
      .state('cp-import', {
        url: '/import',
        templateUrl: 'modules/biospecimen/cp/import.html',
        controller: 'CpImportCtrl',
        parent: 'cps'
      })
      .state('import-multi-cp-objs', {
        url: '/import-multi-cp-objs',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          cp: function(CollectionProtocol) {
            return new CollectionProtocol({id: -1});
          },

          allowedEntityTypes: function(cpsCtx) {
            var entityTypes = [];
            if (cpsCtx.participantImportAllowed) {
              entityTypes = entityTypes.concat(['CommonParticipant', 'Participant']);
            }

            if (cpsCtx.visitSpecimenImportAllowed) {
              entityTypes = entityTypes.concat(['SpecimenCollectionGroup', 'Specimen', 'SpecimenEvent']);
            }

            return entityTypes;
          },

          forms: function(cp, allowedEntityTypes) {
            return allowedEntityTypes.length > 0 ? cp.getForms(allowedEntityTypes) : [];
          },

          importDetail: function(cp, allowedEntityTypes, forms, ImportUtil) {
            return ImportUtil.getImportDetail(cp, allowedEntityTypes, forms);
          }
        },
        parent: 'cps'
      })
      .state('import-multi-cp-jobs', {
        url: '/import-multi-cp-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [
                {state: 'cp-list', title: "cp.list"}
              ],
              title: 'bulk_imports.jobs_list',
              objectTypes: [
                "cprMultiple", 'otherCpr', 'cpr', 'participant', 'consent', 'visit',
                'specimen', 'specimenDerivative', 'specimenAliquot',
                'masterSpecimen', 'specimenDisposal', 'extensions'
              ],
              objectParams: {cpId: -1}
            }
          }
        },
        parent: 'cps'
      })
      .state('export-multi-cp-objs', {
        url: '/export-multi-cp-objs',
        templateUrl: 'modules/common/export/add.html',
        controller: 'AddEditExportJobCtrl',
        resolve: {
          cp: function(CollectionProtocol) {
            return new CollectionProtocol({id: -1});
          },

          allowedEntityTypes: function(cp, cpsCtx) {
            var entityTypes = [];
            if (cpsCtx.participantExportAllowed) {
              entityTypes = entityTypes.concat(['CommonParticipant', 'Participant']);
            }

            if (cpsCtx.visitSpecimenExportAllowed) {
              entityTypes.push('SpecimenCollectionGroup');
            }

            if (cpsCtx.visitSpecimenExportAllowed) {
              entityTypes.push('Specimen');
              entityTypes.push('SpecimenEvent');
            }

            return entityTypes;
          },

          forms: function(cp, allowedEntityTypes) {
            return allowedEntityTypes.length > 0 ? cp.getForms(allowedEntityTypes) : [];
          },

          exportDetail: function(cp, allowedEntityTypes, forms, ExportUtil) {
            return ExportUtil.getExportDetail(cp, allowedEntityTypes, forms);
          }
        },
        parent: 'cps'
      })
      .state('cp-detail', {
        url: '/:cpId',
        templateUrl: 'modules/biospecimen/cp/detail.html',
        parent: 'cps',
        resolve: {
          cp: function($stateParams, CollectionProtocol) {
            return CollectionProtocol.getById($stateParams.cpId);
          }
        },
        controller: 'CpDetailCtrl',
        breadcrumb: {
          title: '{{cp.title}}',
          state: 'cp-detail.overview'
        }
      })
      .state('cp-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/biospecimen/cp/overview.html',
        parent: 'cp-detail'
      })
      .state('cp-detail.consents', {
        url: '/consents',
        templateUrl: 'modules/biospecimen/cp/consents.html',
        parent: 'cp-detail',
        resolve: {
          consentTiers: function(cp) {
            return cp.getConsentTiers();
          }
        },
        controller: 'CpConsentsCtrl'
      })
      .state('cp-detail.events', {
        templateUrl: 'modules/biospecimen/cp/events.html',
        parent: 'cp-detail',
        resolve: {
          events: function($stateParams, cp, CollectionProtocolEvent) {
            return CollectionProtocolEvent.listFor(cp.id);
          }
        },
        controller: 'CpEventsCtrl'
      })
      .state('cp-detail.specimen-requirements', {
        url: '/specimen-requirements?eventId',
        templateUrl: 'modules/biospecimen/cp/specimens.html',
        parent: 'cp-detail.events',
        resolve: {
          specimenRequirements: function($stateParams, SpecimenRequirement) {
            var eventId = $stateParams.eventId;
            if (!eventId) {
              return [];
            }

            return SpecimenRequirement.listFor(eventId);
          }
        },
        controller: 'CpSpecimensCtrl'
      })
      .state('cp-detail.settings', {
        url: '/settings',
        abstract: true,
        template: '<div class="clearfix">' +
                  '  <div class="col-xs-12">' +
                  '    <div ui-view></div>' +
                  '  </div>' +
                  '</div>',
        parent: 'cp-detail'
      })
      .state('cp-detail.settings.labels', {
        url: '/label',
        templateUrl: 'modules/biospecimen/cp/label-settings.html',
        parent: 'cp-detail.settings',
        controller: 'CpLabelSettingsCtrl'
      })
      .state('cp-detail.settings.forms', {
        url: '/forms',
        templateUrl: 'modules/biospecimen/cp/form-settings.html',
        parent: 'cp-detail.settings',
        resolve: {
          forms: function(cp) {
            return cp.getForms(['CommonParticipant', 'Participant', 'SpecimenCollectionGroup', 'Specimen']);
          },

          formsWf: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflow(cp.id, 'forms');
          }
        },
        controller: 'CpFormSettingsCtrl'
      })
      .state('cp-detail.settings.catalog', {
        url: '/catalog',
        templateUrl: 'modules/biospecimen/cp/catalog-settings.html',
        parent: 'cp-detail.settings',
        resolve: {
          catalogSetting: function(cp) {
            if (cp.catalogSetting) {
              return cp.catalogSetting;
            }

            return cp.getCatalogSetting().then(
              function(setting) {
                cp.catalogSetting = setting || {};
              }
            );
          }
        },
        controller: 'CpCatalogSettingsCtrl'
      })
      .state('cp-detail.settings.container', {
        url: '/container',
        templateUrl: 'modules/biospecimen/cp/container-settings.html',
        parent: 'cp-detail.settings',
        controller: 'CpContainerSettingsCtrl'
      })
      .state('cp-detail.settings.reporting', {
        url: '/reporting',
        templateUrl: 'modules/biospecimen/cp/report-settings.html',
        parent: 'cp-detail.settings',
        resolve: {
          reportSettings: function(cp) {
            if (cp.reportSettings) {
              return cp.reportSettings;
            }

            return cp.getReportSettings().then(
              function(settings) {
                cp.reportSettings = settings || {enabled: true};
              }
            );
          }
        },
        controller: 'CpReportSettingsCtrl'
      })
      .state('cp-detail.settings.dp', {
        url: '/dp',
        templateUrl: 'modules/biospecimen/cp/dp-settings.html',
        parent: 'cp-detail.settings',
        controller: 'CpDpSettingsCtrl'
      });
    })

    .run(function(UrlResolver) {
      UrlResolver.regUrlState('cp-overview', 'cp-detail.overview', 'cpId');
    });
  
