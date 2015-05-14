package com.example.star.single_io;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.LinkedList;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private int sampleRateInHz = 8000;
    private int channelInConfig =  AudioFormat.CHANNEL_IN_MONO;
    private int channelOutConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    //开始及退出的按钮
    private Button bt_start;
    private Button bt_exit;
    private Spinner spinner_sampleRate;


    protected int m_in_buf_size;         //AudioRecord 写入缓冲区的大小
    private AudioRecord m_in_rec;        //录制音频对象
    private byte[] m_in_bytes;           //录入的字节数组
    private LinkedList<byte[]> m_in_q;   //存放录入字节数组的大小


    private int m_out_buf_size;          //AudioTrack 播放缓冲大小
    private AudioTrack m_out_trk;        //播放音频对象
    private byte[] m_out_bytes;          //播放的字节数组


    private Thread record;               //录制音频线程
    private Thread play;                 //播放音频线程


    private boolean flag = true;         //让线程停止的标志

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("音频输入输出测试");

        bt_start = (Button)findViewById(R.id.start);
        bt_exit  = (Button)findViewById(R.id.end);
        spinner_sampleRate = (Spinner)findViewById(R.id.spinnerSampleRate);
        bt_start.setOnClickListener(this);
        bt_exit.setOnClickListener(this);
        spinner_sampleRate.setOnItemSelectedListener(new OnItemSelectedListenerImpl());
        bt_exit.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start:
                audioParaSet();
                startRecord();
                break;
            case R.id.end:
                exit();
                break;
        }
    }

    public void audioParaSet(){
        // AudioRecord 得到录制最小缓冲区的大小
        m_in_buf_size = AudioRecord.getMinBufferSize(sampleRateInHz,channelInConfig,audioFormat);
        // 实例化播放音频对象
        m_in_rec = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz,
                channelInConfig,audioFormat, m_in_buf_size);
        // 实例化一个字节数组，长度为最小缓冲区的长度
        m_in_bytes = new byte[m_in_buf_size];
        // 实例化一个链表，用来存放字节组数
        m_in_q = new LinkedList<byte[]>();

        // AudioTrack 得到播放最小缓冲区的大小
        m_out_buf_size = AudioTrack.getMinBufferSize(sampleRateInHz,channelOutConfig,audioFormat);
        // 实例化播放音频对象
        m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                channelOutConfig,audioFormat, m_out_buf_size,AudioTrack.MODE_STREAM);
        // 实例化一个长度为播放最小缓冲大小的字节数组
        m_out_bytes = new byte[m_out_buf_size];
    }

    public void startRecord(){
        flag = true;
        record = new Thread(new recordSound());
        play = new Thread(new playRecord());
        // 启动录制线程
        record.start();
        // 启动播放线程
        play.start();

        bt_start.setVisibility(View.INVISIBLE);
        bt_exit.setVisibility(View.VISIBLE);
    }

    class recordSound implements Runnable
    {
        @Override
        public void run()
        {
            Log.i(TAG, "........recordSound run()......");
            byte[] bytes_pkg;
            // 开始录音
           // m_in_rec.stop();
            m_in_rec.startRecording();

            while (flag)
            {
                m_in_rec.read(m_in_bytes, 0, m_in_buf_size);
                bytes_pkg = m_in_bytes.clone();
                Log.i(TAG, "........recordSound bytes_pkg==" + bytes_pkg.length);
                if (m_in_q.size() >= 2)
                {
                    m_in_q.removeFirst();
                }
                m_in_q.add(bytes_pkg);
            }
        }

    }

    public void exit(){
        bt_start.setVisibility(View.VISIBLE);
        bt_exit.setVisibility(View.INVISIBLE);
        flag = false;
        m_in_rec.stop();
        m_in_rec.release();
        m_in_rec = null;
        m_out_trk.stop();
        m_out_trk = null;
    }

    /**
     *  描述 :播放线程
     */
    class playRecord implements Runnable
    {
        @Override
        public void run()
        {
            // TODO Auto-generated method stub
            Log.i(TAG, "........playRecord run()......");
            byte[] bytes_pkg = null;
            // 开始播放
            m_out_trk.play();

            while (flag)
            {
                try
                {
                    m_out_bytes = m_in_q.getFirst();
                    bytes_pkg = m_out_bytes.clone();
                    m_out_trk.write(bytes_pkg, 0, bytes_pkg.length);
                    Log.i(TAG, "........recordSound bytes_pkg==" + bytes_pkg.length);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //下拉框选择事件
    private class OnItemSelectedListenerImpl implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            String selSampleRate = parent.getItemAtPosition(position).toString();
            sampleRateInHz = Integer.parseInt(selSampleRate);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // TODO Auto-generated method stub
        }

    }
}
