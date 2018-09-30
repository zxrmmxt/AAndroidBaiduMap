package com.xt.m_baidu_map;

import android.os.Handler;
import android.widget.SeekBar;

import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.model.LatLng;

import java.util.List;

/**
 * Created by xuti on 2018/7/20.
 * 轨迹回放
 */
public abstract class TrackPlayControl {
    private SeekBar mSeekBar;
    private boolean isPlaying = false;
    private List<LatLng> mLatLngs;
    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            int progress = mSeekBar.getProgress();
            if (progress == mSeekBar.getMax()) {
                stopPlay();
            } else {
                mSeekBar.setProgress(progress + 1);
                onMoveMarker(mLatLngs.get(progress), mLatLngs.get(progress + 1));
//                mHandler.postDelayed(mRunnable, 80);
            }
        }
    };


    public TrackPlayControl(SeekBar seekBar) {
        mSeekBar = seekBar;
    }

    public void init(final List<LatLng> latLngs) {
        mLatLngs = latLngs;
        stopPlay();
        mSeekBar.setMax(latLngs.size() - 1);
        mSeekBar.setProgress(0);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                onDrawMovePoint(trackPoints.get(progress));
                if (!isPlaying) {
                    if (progress < seekBar.getMax())
                        TrackPlayControl.this.onProgressChanged(mLatLngs.get(progress), mLatLngs.get(progress + 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopPlay();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
//        startPlay();
    }

    public void onClickPlay() {
        if (isPlaying) {
            stopPlay();
        } else {
            startPlay();
        }
    }

    public void startPlay() {
        setPlaying(true);
        if (mSeekBar.getProgress() == mSeekBar.getMax()) {
            mSeekBar.setProgress(0);
        }
        doTask();
    }

    public void stopPlay() {
        setPlaying(false);
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
        updatePlayButton(isPlaying);
    }

    protected abstract void onMoveMarker(LatLng startPoint, LatLng endPoint);

    protected abstract void updatePlayButton(boolean isPlaying);

    protected abstract void onDrawMovePoint(LatLng trackPoint);

    protected abstract void onProgressChanged(LatLng startpoint, LatLng endPoint);

    public void backPlay() {
        stopPlay();
        int progress = mSeekBar.getProgress();
        if (progress == 0) {
        } else {
            mSeekBar.setProgress(progress - 1);
        }
    }

    public void nextPlay() {
        stopPlay();
        int progress = mSeekBar.getProgress();
        if (progress == mSeekBar.getMax()) {
        } else {
            mSeekBar.setProgress(progress + 1);
        }
    }

    public void doTask() {
        if (isPlaying) {
            mHandler.post(mRunnable);
        }
    }

    public void doOnMoveMarker(final BDMapUtil bdMapUtil, final LatLng startPoint, final LatLng endPoint) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Marker moveMarker = bdMapUtil.getMoveMarker();
                if (moveMarker == null) {
                    return;
                }
                moveMarker.setPosition(startPoint);
//                        XTLogUtil.d("轨迹回放-------开始点---"+startPoint);
                moveMarker.setRotate((float) MBaiduCommonUtil.getAngle(startPoint, endPoint));
                        /*mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // refresh marker's rotate
                                moveMarker.setRotate((float) CommonUtil.getAngle(startPoint, endPoint));
//                                moveMarker.setRotate(360-startPoint.getDirection());
                            }
                        });*/
                // 是不是正向的标示
                boolean isReverse = (startPoint.latitude > endPoint.latitude);
                final double slope = MBaiduCommonUtil.getSlope(startPoint, endPoint);
                double interception = MBaiduCommonUtil.getInterception(slope, startPoint);
                double xMoveDistance = MBaiduCommonUtil.getXMoveDistance(slope);
                xMoveDistance = isReverse ? xMoveDistance :
                        -1 * xMoveDistance;
                for (double j = startPoint.latitude; !((j > endPoint.latitude) ^ isReverse);
                     j = j - xMoveDistance) {
                    LatLng latLng;
                    if (slope == Double.MAX_VALUE) {
                        latLng = new LatLng(j, startPoint.longitude);
                    } else {
                        latLng = new LatLng(j, (j - interception) / slope);
                    }

                    final LatLng finalLatLng = latLng;
                    moveMarker.setPosition(finalLatLng);
//                            XTLogUtil.d("轨迹回放-------经过点---"+finalLatLng);
                            /*mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    moveMarker.setPosition(finalLatLng);
                                }
                            });*/
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                doTask();
            }
        }).start();
    }

    protected void doOnProgressChanged(BDMapUtil bdMapUtil, final LatLng startPoint, final LatLng endPoint) {
        final Marker moveMarker = bdMapUtil.getMoveMarker();
        moveMarker.setPosition(startPoint);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // refresh marker's rotate
//                                moveMarker.setRotate((float) CommonUtil.getAngle(startPointLocation, endPointLocation));
//                        moveMarker.setRotate(360-trackPoint.getDirection());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // refresh marker's rotate
                        moveMarker.setRotate((float) MBaiduCommonUtil.getAngle(startPoint, endPoint));
//                                moveMarker.setRotate(360-startPoint.getDirection());
                    }
                });
            }
        });
    }
}
