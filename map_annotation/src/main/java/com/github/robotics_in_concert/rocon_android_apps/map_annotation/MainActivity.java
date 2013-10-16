/*
 * Copyright (C) 2013 OSRF.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.robotics_in_concert.rocon_android_apps.map_annotation;

import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import map_store.ListMapsResponse;
import map_store.MapListEntry;
import map_store.PublishMapResponse;

import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.ExpandableListAdapter;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Marker;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Annotation;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Column;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Pickup;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Table;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.Wall;
import com.github.rosjava.android_apps.application_management.RosAppActivity;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;
import org.ros.address.InetAddressFactory;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.exception.RemoteException;
import org.ros.time.NtpTimeProvider;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends RosAppActivity {

	private static final String MAP_FRAME = "map";
	private static final String ROBOT_FRAME = "base_link";
    private static final String mapTopic = "map";
    private static final String scanTopic = "scan";

    private VisualizationView mapView;
	private ViewGroup mainLayout;
	private ViewGroup sideLayout;
	private Button backButton;
	private Button chooseMapButton;
	private MapAnnotationLayer annotationLayer;
	private ProgressDialog waitingDialog;
	private AlertDialog chooseMapDialog;
	private NodeMainExecutor nodeMainExecutor;
	private NodeConfiguration nodeConfiguration;
    private ExpandableListAdapter annotationsList;
    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            annotationsList.notifyDataSetChanged();
            super.handleMessage(msg);
        }
    };

	public MainActivity() {
		// The RosActivity constructor configures the notification title and ticker messages.
		super("Map annotation", "Map annotation");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		String defaultRobotName = getString(R.string.default_robot);
		String defaultAppName = getString(R.string.default_app);
		setDefaultRobotName(defaultRobotName);
		setDefaultAppName(defaultAppName);
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		mapView = (VisualizationView) findViewById(R.id.map_view);
		backButton = (Button) findViewById(R.id.back_button);
		chooseMapButton = (Button) findViewById(R.id.choose_map_button);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});

		chooseMapButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onChooseMapButtonPressed();
			}
		});

		mapView.getCamera().jumpToFrame(ROBOT_FRAME);
		mainLayout = (ViewGroup) findViewById(R.id.main_layout);
		sideLayout = (ViewGroup) findViewById(R.id.side_layout);

        // Configure the ExpandableListView and its adapter containing current annotations
        ExpandableListView listView = (ExpandableListView) findViewById(R.id.annotations_view);
//        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
//        {
//            @Override
//            public boolean onChildClick(ExpandableListView arg0, View arg1, int arg2, int arg3, long arg4)
//            {
//                Toast.makeText(getBaseContext(), "Child clicked", Toast.LENGTH_LONG).show();
//                return false;
//            }
//        });
//
//        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener()
//        {
//            @Override
//            public boolean onGroupClick(ExpandableListView arg0, View arg1, int arg2, long arg3)
//            {
//                Toast.makeText(getBaseContext(), "Group clicked", Toast.LENGTH_LONG).show();
//                return false;
//            }
//        });

        annotationsList = new ExpandableListAdapter(this, listView);
        listView.setAdapter(annotationsList);

        // TODO use reflection to take all classes on annotations package except Annotation
        annotationsList.addGroup(new Marker(""));
        annotationsList.addGroup(new Pickup(""));
        annotationsList.addGroup(new Table(""));
        annotationsList.addGroup(new Column(""));
        annotationsList.addGroup(new Wall(""));
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {

		super.init(nodeMainExecutor);
		
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());

		NameResolver appNameSpace = getAppNameSpace();

		ViewControlLayer viewControlLayer = new ViewControlLayer(this,
				nodeMainExecutor.getScheduledExecutorService(), mapView, mainLayout, sideLayout);

		viewControlLayer.addListener(new CameraControlListener() {
            @Override
            public void onZoom(double focusX, double focusY, double factor) {

            }

            @Override
            public void onTranslate(float distanceX, float distanceY) {

            }

            @Override
            public void onRotate(double focusX, double focusY, double deltaAngle) {

            }
        });

		mapView.addLayer(viewControlLayer);
		mapView.addLayer(new OccupancyGridLayer(appNameSpace.resolve(mapTopic).toString()));
        annotationLayer = new MapAnnotationLayer(appNameSpace, this, annotationsList);
		mapView.addLayer(annotationLayer);
		NtpTimeProvider ntpTimeProvider = new NtpTimeProvider(
				InetAddressFactory.newFromHostString("192.168.0.1"),
				nodeMainExecutor.getScheduledExecutorService());
		ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
		nodeConfiguration.setTimeProvider(ntpTimeProvider);
		nodeMainExecutor.execute(mapView, nodeConfiguration.setNodeName("android/map_view"));

		readAvailableMapList();
	}

	private void onChooseMapButtonPressed() {
		readAvailableMapList();
	}

	public void addMarkerClicked(View view) {
        annotationLayer.setMode(MapAnnotationLayer.Mode.ADD_MARKER);
	}

    public void addColumnClicked(View view) {
        annotationLayer.setMode(MapAnnotationLayer.Mode.ADD_COLUMN);
    }

    public void addWallClicked(View view) {
        annotationLayer.setMode(MapAnnotationLayer.Mode.ADD_WALL);
    }

    public void addTableClicked(View view) {
        annotationLayer.setMode(MapAnnotationLayer.Mode.ADD_TABLE);
    }

    public void addPickupClicked(View view) {
        annotationLayer.setMode(MapAnnotationLayer.Mode.ADD_PICKUP);
    }

	private void readAvailableMapList() {
		safeShowWaitingDialog("Waiting...", "Waiting for map list");

		MapManager mapManager = new MapManager();
        mapManager.setNameResolver(getAppNameSpace());
		mapManager.setFunction("list");
		safeShowWaitingDialog("Waiting...", "Waiting for map list");
		mapManager.setListService(new ServiceResponseListener<ListMapsResponse>() {
					@Override
					public void onSuccess(ListMapsResponse message) {
						Log.i("MapAnn", "readAvailableMapList() Success");
						safeDismissWaitingDialog();
						showMapListDialog(message.getMapList());
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.i("MapAnn", "readAvailableMapList() Failure");
						safeDismissWaitingDialog();
					}
				});

		nodeMainExecutor.execute(mapManager,
				nodeConfiguration.setNodeName("android/list_maps"));
	}

	/**
	 * Show a dialog with a list of maps. Safe to call from any thread.
	 */
	private void showMapListDialog(final List<MapListEntry> list) {
		// Make an array of map name/date strings.
		final CharSequence[] availableMapNames = new CharSequence[list.size()];
		for (int i = 0; i < list.size(); i++) {
			String displayString;
			String name = list.get(i).getName();
			Date creationDate = new Date(list.get(i).getDate() * 1000);
			String dateTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.SHORT).format(creationDate);
			if (name != null && !name.equals("")) {
				displayString = name + " " + dateTime;
			} else {
				displayString = dateTime;
			}
			availableMapNames[i] = displayString;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				builder.setTitle("Choose a map");
				builder.setItems(availableMapNames,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int itemIndex) {
								loadMap(list.get(itemIndex));
							}
						});
				chooseMapDialog = builder.create();
				chooseMapDialog.show();
			}
		});
	}

	private void loadMap(MapListEntry mapListEntry) {

		MapManager mapManager = new MapManager();
        mapManager.setNameResolver(getAppNameSpace());
		mapManager.setFunction("publish");
		mapManager.setMapId(mapListEntry.getMapId());

		safeShowWaitingDialog("Waiting...", "Loading map");
		try {
			mapManager
					.setPublishService(new ServiceResponseListener<PublishMapResponse>() {
						@Override
						public void onSuccess(PublishMapResponse message) {
							Log.i("MapAnn", "loadMap() Success");
							safeDismissWaitingDialog();
							// poseSetter.enable();
						}

						@Override
						public void onFailure(RemoteException e) {
							Log.i("MapAnn", "loadMap() Failure");
							safeDismissWaitingDialog();
						}
					});
		} catch (Throwable ex) {
			Log.e("MapAnn", "loadMap() caught exception.", ex);
			safeDismissWaitingDialog();
		}
		nodeMainExecutor.execute(mapManager,
				nodeConfiguration.setNodeName("android/publish_map"));
	}

	private void safeDismissChooseMapDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (chooseMapDialog != null) {
					chooseMapDialog.dismiss();
					chooseMapDialog = null;
				}
			}
		});
	}

	private void showWaitingDialog(final CharSequence title,
			final CharSequence message) {
		dismissWaitingDialog();
		waitingDialog = ProgressDialog.show(MainActivity.this, title, message,
				true);
		waitingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	}

	private void dismissWaitingDialog() {
		if (waitingDialog != null) {
			waitingDialog.dismiss();
			waitingDialog = null;
		}
	}

	private void safeShowWaitingDialog(final CharSequence title,
			final CharSequence message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showWaitingDialog(title, message);
			}
		});
	}

	private void safeDismissWaitingDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dismissWaitingDialog();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.stop_app);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case 0:
			onDestroy();
			break;
		}
		return true;
	}
}