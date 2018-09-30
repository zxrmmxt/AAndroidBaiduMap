package com.xt.m_baidu_map;

import android.content.Context;
import android.support.annotation.NonNull;

import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.api.analysis.DrivingBehaviorRequest;
import com.baidu.trace.api.analysis.OnAnalysisListener;
import com.baidu.trace.api.analysis.StayPointRequest;
import com.baidu.trace.api.entity.EntityListRequest;
import com.baidu.trace.api.entity.FilterCondition;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.LatestPointRequest;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.SupplementMode;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.TransportMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuti on 2017/7/13.
 * 1，查询历史轨迹的经纬度坐标
 * 2，显示轨迹
 */

public class BaiduTrackUtils {
    private static BaiduTrackUtils instance;
    private LBSTraceClient mTraceClient;
    private OnTrackListener mTrackListener;
    private long mServiceId;

    /**
     * 在activity的setContentView方法之前调用
     */
//    public static void init(Context applicationContext){
//        SDKInitializer.initialize(applicationContext);
//    }
    private BaiduTrackUtils(Context context, long serviceId) {
        initTraceService(context, serviceId);
        setLocationAndReturnCycle();
        initListener();
        MBaiduBitmapUtil.init();
    }

    public static BaiduTrackUtils getInstance(Context context, long serviceId) {
        if (instance == null) {
            instance = new BaiduTrackUtils(context, serviceId);
        }
        return instance;
    }

    private void initTraceService(Context context, long serviceId) {
        // 轨迹服务ID
        mServiceId = serviceId;
        // 设备标识
        String entityName = "myTrace";
        // 是否需要对象存储服务，默认为：false，关闭对象存储服务。注：鹰眼 Android SDK v3.0以上版本支持随轨迹上传图像等对象数据，若需使用此功能，该参数需设为 true，且需导入bos-android-sdk-1.0.2.jar。
        boolean isNeedObjectStorage = false;
        // 初始化轨迹服务
        Trace mTrace = new Trace(mServiceId, entityName, isNeedObjectStorage);
        // 初始化轨迹服务客户端
        mTraceClient = new LBSTraceClient(context);
    }

    public void setLocationAndReturnCycle() {
        //鹰眼轨迹数据传输采取定期打包回传的方式，以节省流量。开发者可以自定义定位频率和打包回传频率，频率可设置范围为：2~300秒。例如：定位频率为5s，打包回传频率为10s，则2次定位后打包回传一次。
        // 定位周期(单位:秒)
        int gatherInterval = 5;
        // 打包回传周期(单位:秒)
        int packInterval = 10;
        // 设置定位和打包周期
        mTraceClient.setInterval(gatherInterval, packInterval);
    }

    public void initListener() {

        mTrackListener = new OnTrackListener() {
            // 历史轨迹回调
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse response) {
                final List<TrackPoint> trackPoints = response.getTrackPoints();
            }
        };
    }

    @NonNull
    public static List<TrackPoint> getAvaibleTrackPoints(List<TrackPoint> trackPoints) {
        /**
         * 轨迹点集合
         */
        List<TrackPoint> trackPointList = new ArrayList<>();
        if (null != trackPoints) {
            for (TrackPoint trackPoint : trackPoints) {
                if (!MBaiduCommonUtil.isZeroPoint(trackPoint.getLocation().getLatitude(),
                        trackPoint.getLocation().getLongitude())) {
                    trackPointList.add(trackPoint);
                }
            }
        }
        return trackPointList;
    }

    public static List<LatLng> getLatLngList(List<TrackPoint> trackPoints) {
        List<LatLng> latLngs = new ArrayList<>();
        for (TrackPoint item : trackPoints) {
            latLngs.add(BDMapUtil.convertTrace2Map(item.getLocation()));
        }
        return latLngs;
    }

    public static List<List<LatLng>> getLatLngListList(List<List<TrackPoint>> trackPoints) {
        List<List<LatLng>> latLngListList = new ArrayList<>();
        for (List<TrackPoint> item : trackPoints) {
            latLngListList.add(getLatLngList(item));
        }
        return latLngListList;
    }

    public static List<List<TrackPoint>> splitTrackPoints(List<TrackPoint> trackPoints) {
        int trackPointSize = trackPoints.size();
        if (trackPointSize > 0) {
            List<List<TrackPoint>> paths = new ArrayList<>();
            List<TrackPoint> tempTrackPoints = new ArrayList<>();
            for (int i = 0; i <= trackPointSize - 1; i++) {
                TrackPoint currentTrackPoint = trackPoints.get(i);
                tempTrackPoints.add(currentTrackPoint);
                if (i == trackPointSize - 1) {
                    paths.add(new ArrayList<>(tempTrackPoints));
                } else {
                    long currentLocTime = currentTrackPoint.getLocTime();
                    TrackPoint nextTrackPoint = trackPoints.get(i + 1);
                    long nextLocTime = nextTrackPoint.getLocTime();
                    if ((nextLocTime * 1000 - currentLocTime * 1000) > 5 * 60 * 1000) {
//                            addSubTrack(paths, trackPoints.subList(fromIndex, toIndex));
                        if (tempTrackPoints.size() > 0) {
                            paths.add(new ArrayList<>(tempTrackPoints));
                        }
                        tempTrackPoints.clear();
                    }
                }
            }
            return paths;
        } else {
            return null;
        }
    }

    private ProcessOption getProcessOption(boolean isNeedDenoise, boolean isNeedVacuate, boolean isNeedMapMatch, int radiusThreshold, TransportMode transportMode) {
        // 创建纠偏选项实例
        ProcessOption processOption = new ProcessOption();
        // 设置需要去噪
        processOption.setNeedDenoise(isNeedDenoise);
        // 设置需要抽稀
        processOption.setNeedVacuate(isNeedVacuate);
        // 设置需要绑路
        processOption.setNeedMapMatch(isNeedMapMatch);
        // 设置精度过滤值(定位精度大于100米的过滤掉)
        processOption.setRadiusThreshold(radiusThreshold);
        // 设置交通方式为驾车
        processOption.setTransportMode(transportMode);
        return processOption;
    }

    public void queryHistoryTrack(String sn, long startTime, long endTime, OnTrackListener onTrackListener) {
        //轨迹上传的同时， 可通过轨迹查询接口获取实时轨迹信息：
        // 请求标识
        int tag = 1;
        // 轨迹服务ID
        long serviceId = mServiceId;
        // 设备标识
        String entityName = sn;
        // 创建历史轨迹请求实例
        HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest(tag, serviceId, entityName);

        //设置轨迹查询起止时间
        // 开始时间(单位：秒)
//        long startTime = System.currentTimeMillis() / 1000 - 12 * 60 * 60;
        // 结束时间(单位：秒)
//        long endTime = System.currentTimeMillis() / 1000;
        // 设置开始时间
        historyTrackRequest.setStartTime(startTime);
        historyTrackRequest.setPageSize(5000);
        // 设置结束时间
        historyTrackRequest.setEndTime(endTime);
        //是否返回纠偏后的轨迹
        historyTrackRequest.setProcessed(true);
        // 设置纠偏选项
        ProcessOption processOption = getProcessOption(true, true, true, 20, TransportMode.driving);
//        ProcessOption processOption = getProcessOption(true, true, false, 20);
        historyTrackRequest.setProcessOption(processOption);

        // 设置里程填充方式为驾车
        historyTrackRequest.setSupplementMode(SupplementMode.driving);
        // 查询历史轨迹
        mTraceClient.queryHistoryTrack(historyTrackRequest, onTrackListener);
    }

    public void queryLatestPoint(String sn, OnTrackListener onTrackListener) {
        //轨迹上传的同时， 可通过轨迹查询接口获取实时轨迹信息：
        // 请求标识
        int tag = 2;
        // 轨迹服务ID
        long serviceId = mServiceId;
        // 设备标识
        String entityName = sn;
        // 创建历史轨迹请求实例
        LatestPointRequest latestPointRequest = new LatestPointRequest(tag, serviceId, entityName);
        latestPointRequest.setCoordTypeOutput(CoordType.bd09ll);
        //设置轨迹查询起止时间
        // 开始时间(单位：秒)
//        long startTime = System.currentTimeMillis() / 1000 - 12 * 60 * 60;
        // 结束时间(单位：秒)
//        long endTime = System.currentTimeMillis() / 1000;
        // 设置纠偏选项
        ProcessOption processOption = new ProcessOption();
        processOption.setTransportMode(TransportMode.driving);
        latestPointRequest.setProcessOption(processOption);
        mTraceClient.queryLatestPoint(latestPointRequest, onTrackListener);
    }

    //查询实时位置
    public void queryEntityList(List<String> snList, OnEntityListener entityListener) {
        //轨迹上传的同时， 可通过轨迹查询接口获取实时轨迹信息：
        // 请求标识
        int tag = 3;
        // 轨迹服务ID
        long serviceId = mServiceId;
        // 设备标识 entity标识列表（多个entityName，以英文逗号"," 分割）
        List<String> entityNames = snList;
        //检索条件（格式为 : "key1=value1,key2=value2,....."）
//        Map<String,String> columns = new HashMap<>();
        //返回结果的类型（0 : 返回全部结果，1 : 只返回entityName的列表）
        int returnType = 0;
        //活跃时间，UNIX时间戳（指定该字段时，返回从该时间点之后仍有位置变动的entity的实时点集合）
//        int activeTime = (int) (System.currentTimeMillis() / 1000 - 12 * 60 * 60);
//        int activeTime = (int) (System.currentTimeMillis() / 1000);
        //分页大小
        int pageSize = 100;
        //分页索引
        int pageIndex = 0;
        EntityListRequest entityListRequest = new EntityListRequest(tag, serviceId);
        FilterCondition filterCondition = new FilterCondition();
        filterCondition.setEntityNames(entityNames);
//        filterCondition.setColumns(columns);
//        filterCondition.setActiveTime(activeTime);
        entityListRequest.setFilterCondition(filterCondition);
        entityListRequest.setPageSize(pageSize);
        entityListRequest.setPageIndex(pageIndex);
        mTraceClient.queryEntityList(entityListRequest, entityListener);
    }

    public void queryDrivingBehavior(String sn, long startTime, long endTime, OnAnalysisListener onAnalysisListener) {
        // 请求标识
        int tag = 4;
        // 轨迹服务ID
        long serviceId = mServiceId;
        // 设备标识
        String entityName = sn;
        DrivingBehaviorRequest drivingBehaviorRequest = new DrivingBehaviorRequest(tag, serviceId, entityName);
        drivingBehaviorRequest.setStartTime(startTime / 1000);
        drivingBehaviorRequest.setEndTime(endTime / 1000);
        drivingBehaviorRequest.setCoordTypeOutput(CoordType.bd09ll);
//        ThresholdOption thresholdOption = new ThresholdOption();
//        drivingBehaviorRequest.setThresholdOption(thresholdOption);
        drivingBehaviorRequest.setProcessOption(
                getProcessOption(
                        true,
                        true,
                        true,
                        20,
                        TransportMode.driving));
        mTraceClient.queryDrivingBehavior(drivingBehaviorRequest, onAnalysisListener);
    }

    public void queryStayPoint(String sn, long startTime, long endTime, OnAnalysisListener onAnalysisListener) {
        // 请求标识
        int tag = 5;
        // 轨迹服务ID
        long serviceId = mServiceId;
        // 设备标识
        String entityName = sn;
        StayPointRequest stayPointRequest = new StayPointRequest(tag, serviceId, entityName);
        stayPointRequest.setStartTime(startTime);
        stayPointRequest.setEndTime(endTime);
        stayPointRequest.setCoordTypeOutput(CoordType.bd09ll);
        stayPointRequest.setStayRadius(20);
        stayPointRequest.setStayTime(100);
        // 设置纠偏选项
        ProcessOption processOption = getProcessOption(true, true, true, 20, TransportMode.driving);
//        ProcessOption processOption = getProcessOption(true, true, false, 20);
        stayPointRequest.setProcessOption(processOption);
        mTraceClient.queryStayPoint(stayPointRequest, onAnalysisListener);
    }
}
