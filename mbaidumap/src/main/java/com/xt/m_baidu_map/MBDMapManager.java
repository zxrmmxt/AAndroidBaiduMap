package com.xt.m_baidu_map;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.GroundOverlay;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.trace.model.SortType;

import java.util.List;

/**
 * Created by baidu on 17/2/9.
 */

public class MBDMapManager {

    public static final float ZOOM_BAIDU_MAP = 15f;//3~21
    private MapStatus mapStatus = null;

    public TextureMapView mapView = null;

    public BaiduMap baiduMap = null;

    public LatLng lastPoint = null;
    public static final String[] LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M"
            , "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    /**
     * 路线覆盖物
     */
    public Overlay polylineOverlay = null;

    public MBDMapManager(TextureMapView view) {
        mapView = view;
        baiduMap = mapView.getMap();
        mapView.showZoomControls(false);
//        setCenter(null);
    }

    public void onPause() {
        if (null != mapView) {
            mapView.onPause();
        }
    }

    public void onResume() {
        if (null != mapView) {
            mapView.onResume();
        }
    }

    public void clear() {
        lastPoint = null;
        if (null != mMoveMarker) {
            mMoveMarker.remove();
            mMoveMarker = null;
        }
        if (null != polylineOverlay) {
            polylineOverlay.remove();
            polylineOverlay = null;
        }
        if (null != baiduMap) {
            baiduMap.clear();
            baiduMap = null;
        }
        mapStatus = null;
        if (null != mapView) {
            mapView.onDestroy();
            mapView = null;
        }
    }

    public void setOnMapLoadedCallback(BaiduMap.OnMapLoadedCallback callback) {
        baiduMap.setOnMapLoadedCallback(callback);
    }

    /**********************************添加地图覆盖物*******************************************/
    public Marker addOverlay(LatLng currentPoint, BitmapDescriptor icon, Bundle bundle) {
        OverlayOptions overlayOptions = new MarkerOptions().position(currentPoint)
                .icon(icon).zIndex(9).animateType(MarkerOptions.MarkerAnimateType.none).draggable(true).perspective(true).flat(false);
        Marker marker = (Marker) baiduMap.addOverlay(overlayOptions);
        if (null != bundle) {
            marker.setExtraInfo(bundle);
        }
        return marker;
    }

    public GroundOverlay addOverlay(LatLng currentPoint, BitmapDescriptor icon, Bundle bundle,int width,int height) {
        OverlayOptions overlayOptions = new GroundOverlayOptions().position(currentPoint)
                .image(icon).zIndex(9).dimensions(width, height);
        GroundOverlay groundOverlay = (GroundOverlay) baiduMap.addOverlay(overlayOptions);
        if (null != bundle) {
            groundOverlay.setExtraInfo(bundle);
        }
        return groundOverlay;
    }

    /**
     * 添加地图覆盖物
     */
//    public void addMarker(LatLng currentPoint) {
//        if (null == mMoveMarker) {
//            mMoveMarker = addOverlay(currentPoint, MBaiduBitmapUtil.bmArrowPoint, null);
//            return;
//        }
//
//        if (null != lastPoint) {
//            moveLooper(currentPoint);
//        } else {
//            lastPoint = currentPoint;
//            mMoveMarker.setPosition(currentPoint);
//        }
//    }


    private Marker mMoveMarker = null;

    public void addMoveMarker(LatLng current, LatLng next) {
        if (mMoveMarker != null) {
            mMoveMarker.remove();
            mMoveMarker = null;
        }
        BitmapDescriptor myLocationIcon = BitmapDescriptorFactory
                .fromResource(R.mipmap.icon_point);
        OverlayOptions markerOptions =
                new MarkerOptions().flat(true).anchor(0.5f, 0.5f).icon(myLocationIcon)
                        .position(current)
                        .rotate((float) MBaiduCommonUtil.getAngle(current, next));
        mMoveMarker = (Marker) baiduMap.addOverlay(markerOptions);
        mMoveMarker.setRotate((float) MBaiduCommonUtil.getAngle(current, next));
    }

    public void addMarker(LatLng currentPoint) {
        BitmapDescriptor myLocationIcon = BitmapDescriptorFactory
                .fromResource(R.mipmap.icon_point);
        MarkerOptions markerOptions = new MarkerOptions().position(currentPoint)
                .icon(myLocationIcon);
        if (mMoveMarker != null) {
            mMoveMarker.remove();
            mMoveMarker = null;
        }
        mMoveMarker = (Marker) baiduMap.addOverlay(markerOptions);
    }

    public Marker getMoveMarker() {
        return mMoveMarker;
    }

    public void clearOverlay() {
        // 绘制新覆盖物前，清空之前的覆盖物
        baiduMap.clear();
        if (null != polylineOverlay) {
            polylineOverlay.remove();
            polylineOverlay = null;
        }
    }
    /**********************************添加地图覆盖物*******************************************/

    /**
     * 移动逻辑
     */
    public void moveLooper(LatLng endPoint) {

        mMoveMarker.setPosition(lastPoint);
        mMoveMarker.setRotate((float) MBaiduCommonUtil.getAngle(lastPoint, endPoint));

        double slope = MBaiduCommonUtil.getSlope(lastPoint, endPoint);
        // 是不是正向的标示（向上设为正向）
        boolean isReverse = (lastPoint.latitude > endPoint.latitude);
        double intercept = MBaiduCommonUtil.getInterception(slope, lastPoint);
        double xMoveDistance = isReverse ? MBaiduCommonUtil.getXMoveDistance(slope) : -1 * MBaiduCommonUtil.getXMoveDistance(slope);

        for (double latitude = lastPoint.latitude; latitude > endPoint.latitude == isReverse; latitude =
                latitude - xMoveDistance) {
            LatLng latLng;
            if (slope != Double.MAX_VALUE) {
                latLng = new LatLng(latitude, (latitude - intercept) / slope);
            } else {
                latLng = new LatLng(latitude, lastPoint.longitude);
            }
            mMoveMarker.setPosition(latLng);
        }
    }

    /*************************************更新地图状态*************************************************/
    public void updateStatus(Context context,LatLng currentPoint, boolean showMarker) {
        if (null == baiduMap || null == currentPoint) {
            return;
        }

        if (null != baiduMap.getProjection()) {
            Point screenPoint = baiduMap.getProjection().toScreenLocation(currentPoint);
            // 点在屏幕上的坐标超过限制范围，则重新聚焦底图
            if (screenPoint.y < 200 || screenPoint.y > getScreenHeight(context) - 500
                    || screenPoint.x < 200 || screenPoint.x > getScreenHeight(context) - 200
                    || null == mapStatus) {
                animateMapStatus(currentPoint, ZOOM_BAIDU_MAP);
            }
        } else if (null == mapStatus) {
            // 第一次定位时，聚焦底图
            setMapStatus(currentPoint, ZOOM_BAIDU_MAP);
        }

        if (showMarker) {
//            addMarker(currentPoint);
        }

    }

    /**
     * 设置地图中心：使用已有定位信息;
     */
    public void setCenter(Context context,LatLng latLng) {
        updateStatus(context,latLng, false);
    }

    private MapStatusUpdate getMapStatusUpdate(LatLng point) {
        MapStatus.Builder builder = new MapStatus.Builder();
        mapStatus = builder.target(point).build();
        return MapStatusUpdateFactory.newMapStatus(mapStatus);
    }

    private MapStatusUpdate getMapStatusUpdate(LatLng point, float zoom) {
        MapStatus.Builder builder = new MapStatus.Builder();
        mapStatus = builder.target(point).zoom(zoom).build();
        return MapStatusUpdateFactory.newMapStatus(mapStatus);
    }

    public void animateMapStatus(LatLng point, float zoom) {
        baiduMap.animateMapStatus(getMapStatusUpdate(point, zoom));
    }

    public void animateMapStatus(LatLng point) {
        baiduMap.animateMapStatus(getMapStatusUpdate(point));
    }

    public void setMapStatus(LatLng point, float zoom) {
        baiduMap.setMapStatus(getMapStatusUpdate(point, zoom));
    }

    public void refresh() {
        LatLng mapCenter = baiduMap.getMapStatus().target;
        float mapZoom = baiduMap.getMapStatus().zoom - 1.0f;
        setMapStatus(mapCenter, mapZoom);
    }

    public void overlook(float angle) {
        MapStatus mapStatus = new MapStatus.Builder().overlook(angle).build();
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mapStatus));
    }

    public void animateMapStatus(List<LatLng> points) {
        if (null == points || points.isEmpty()) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        MapStatusUpdate msUpdate = MapStatusUpdateFactory.newLatLngBounds(builder.build());
        baiduMap.animateMapStatus(msUpdate);
    }
    /*************************************更新地图状态*************************************************/


    /***************************绘制历史轨迹**************************************/
    public Marker[] drawHistoryTrace(List<LatLng> points, int index, int color, String endText) {
        return drawHistoryTrack(points, index, color, SortType.asc, endText);
//        ToastUtils.showShort("绘制历史轨迹");
    }

    /**
     * 绘制历史轨迹
     */
    public Marker[] drawHistoryTrack(List<LatLng> points, int index, int color, SortType sortType, String endText) {
        Marker[] markers = new Marker[2];
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setBuildingsEnabled(false);
        if (points == null || points.size() == 0) {
            return markers;
        }
        String letter;
        if (index < 0 || index > LETTERS.length - 1) {
            letter = "";
        } else {
            letter = LETTERS[index];
        }
        View startView = View.inflate(mapView.getContext(), R.layout.baidu_start_end, null);
        View iv_start = startView.findViewById(R.id.baidu_start_end_iv);
        TextView tv_start = (TextView) startView.findViewById(R.id.baidu_start_end_tv);
        iv_start.setBackgroundResource(R.mipmap.iconstart);
        tv_start.setText(letter);
        BitmapDescriptor startBitmap = BitmapDescriptorFactory.fromView(startView);
        View endView = View.inflate(mapView.getContext(), R.layout.baidu_start_end, null);
        View iv_end = endView.findViewById(R.id.baidu_start_end_iv);
        TextView tv_end = (TextView) endView.findViewById(R.id.baidu_start_end_tv);
        iv_end.setBackgroundResource(R.mipmap.iconend);
        if (TextUtils.isEmpty(endText)) {
            tv_end.setText(letter);
        } else {
            tv_end.setText(endText);
        }
        BitmapDescriptor endBitmap = BitmapDescriptorFactory.fromView(endView);
        String startTitle = "起点-" + letter;
        String endTitle = "终点-" + letter;
        if (points.size() == 1) {
            OverlayOptions startOptions = new MarkerOptions().position(points.get(0)).icon(startBitmap)
                    .zIndex(9).draggable(true).title(startTitle);
            Overlay overlay = baiduMap.addOverlay(startOptions);
            markers[0] = (Marker) overlay;
            animateMapStatus(points.get(0), ZOOM_BAIDU_MAP);
            return markers;
        }
        LatLng startPoint;
        LatLng endPoint;
        if (sortType == SortType.asc) {
            startPoint = points.get(0);
            endPoint = points.get(points.size() - 1);
        } else {
            startPoint = points.get(points.size() - 1);
            endPoint = points.get(0);
        }

        // 添加起点图标
        OverlayOptions startOptions = new MarkerOptions()
                .position(startPoint).icon(startBitmap)
                .zIndex(9).draggable(true).title(startTitle);
        // 添加终点图标
        OverlayOptions endOptions = new MarkerOptions().position(endPoint)
                .icon(endBitmap).zIndex(9).draggable(true).title(endTitle);

        // 添加路线（轨迹）
        OverlayOptions polylineOptions = new PolylineOptions().width(10)
                .color(color).points(points);

        Marker startMarker = (Marker) baiduMap.addOverlay(startOptions);
        Marker endMarker = (Marker) baiduMap.addOverlay(endOptions);
        markers[0] = startMarker;
        markers[1] = endMarker;
        polylineOverlay = baiduMap.addOverlay(polylineOptions);
        return markers;
    }
    /***************************绘制历史轨迹**************************************/

    /*******************************获取屏幕宽高**********************************/
    /**
     * Return the width of screen, in pixel.
     *
     * @return the width of screen, in pixel
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return context.getResources().getDisplayMetrics().widthPixels;
        }
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            wm.getDefaultDisplay().getSize(point);
        }
        return point.x;
    }

    /**
     * Return the height of screen, in pixel.
     *
     * @return the height of screen, in pixel
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return context.getResources().getDisplayMetrics().heightPixels;
        }
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            wm.getDefaultDisplay().getSize(point);
        }
        return point.y;
    }
    /*******************************获取屏幕宽高**********************************/
}

