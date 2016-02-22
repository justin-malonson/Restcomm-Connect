App.controller('projectManagerCtrl', function ( $scope, $http, $location, $routeParams, $timeout, $upload, notifications, authentication) {
	
	$scope.authInfo = authentication.getAuthInfo();
	$scope.projectNameValidator = /^[^:;@#!$%^&*()+|~=`{}\\\[\]"<>?,\/]+$/;
	$scope.projectKind = $routeParams.projectKind;
	if ( $scope.projectKind != 'voice' && $scope.projectKind != 'ussd' && $scope.projectKind != 'sms')
		$scope.projectKind = 'voice';
	$scope.error = undefined; 
	//$scope.notifications = [];

	
	$scope.refreshProjectList = function() {
		$http({url: 'services/projects',
				method: "GET"
		})
		.success(function (data, status, headers, config) {
			$scope.projectList = data;
			for ( var i=0; i < $scope.projectList.length; i ++)
				$scope.projectList[i].viewMode = 'view';
		})
		.error(function (data, status, headers, config) {
			if (status == 500)
				notifications.put({type:'danger',message:"Internal server error"});
		});
	}
	
	$scope.createNewProject = function(name, kind, ticket) {
		$http({url: 'services/projects/' + name + "/?kind=" + kind + "&ticket=" + ticket,
				method: "PUT"
		})
		.success(function (data, status, headers, config) {
			console.log( "project created");
			$location.path("/designer/" + name);
		 })
		 .error(function (data, status, headers, config) {
			if (status == 409) {
				console.log("project already exists");
				notifications.put({type:'danger',message:'A Voice, SMS or USSD project  with that name already exists in the workspace (maybe it belongs to another user).'});
			} else
			if (status >= 500) {
				console.log("internal server error: " + status);
				notifications.put({type:'danger',message:'Internal server error.'});
			} else {
				console.log("operation failed: " + status);
				notifications.put({type:'danger',message:'Could not create project.'});
			}
		 });
	}
	
	
	$scope.editProjectName = function(projectItem) {
		projectItem.viewMode = 'edit';
		projectItem.newProjectName = projectItem.name;
		projectItem.errorMessage = "";
	}
	
	$scope.applyNewProjectName = function(projectItem, ticket) {
		if ( projectItem.name == projectItem.newProjectName ) {
			projectItem.viewMode = 'view';
			return;
		}
		$http({ method: "PUT", url: 'services/projects/' + projectItem.name + '/rename?newName=' + projectItem.newProjectName + "&ticket=" + ticket})
			.success(function (data, status, headers, config) { 
				console.log( "project " + projectItem.name + " renamed to " + projectItem.newProjectName );
				projectItem.name = projectItem.newProjectName;
				projectItem.viewMode = 'view';
				
			})
			.error(function (data, status, headers, config) {
				if (status == 409)
					projectItem.errorMessage = "Project already exists!";
				else
					projectItem.errorMessage = "Cannot rename project";
			});
	}
	
	$scope.deleteProject = function(projectItem, ticket) {
		$http({ method: "DELETE", url: 'services/projects/' + projectItem.name + "?ticket=" + ticket})
		.success(function (data, status, headers, config) { 
			console.log( "project " + projectItem.name + " deleted " );
			$scope.refreshProjectList();
			projectItem.showConfirmation = false;
		})
		.error(function (data, status, headers, config) {
		    console.log("cannot delete project");
		    if (status >= 500) {
		        notifications.put({type:'danger',message:'Internal server error.'});
		    } else {
		        notifications.put({type:'danger',message:'Could not delete project.'});
		    }
		});
	}
	
	$scope.onFileSelect_ImportProject = function($files, ticket) {
	    for (var i = 0; i < $files.length; i++) {
	      var file = $files[i];
	      $scope.upload = $upload.upload({
	        url: 'services/projects?ticket=' + ticket,
	        file: file,
	      }).progress(function(evt) {
	        console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	      }).success(function(data, status, headers, config) {
	    	  console.log('Project imported successfully');
	    	  $scope.refreshProjectList();
	    	  notifications.put({message:"Project imported successfully", type:"success"});
	      }).error(function(data, status, headers, config) {
	    	  if (status == 400) {// BAD REQUEST
	    		  console.log(data.exception.message);
	    		  notifications.put({message:"Error importing project", type:"danger"});
	    	  }
	      });
	    }
	};
	
    $scope.refreshProjectList();	
	
});

