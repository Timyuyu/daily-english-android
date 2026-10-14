package com.dailyenglish.app;

import android.content.Context;
import android.media.*;
import android.os.Handler;
import android.os.Looper;
import com.k2fsa.sherpa.onnx.*;
import java.util.*;
import java.util.concurrent.*;

public final class SpeechPlayer {
    public interface Listener { void changed(String status,int sentence); }
    private final AudioManager audio;
    private final AudioFocusRequest focus;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService synthesis=Executors.newSingleThreadExecutor();
    private final Object trackLock=new Object();
    private final Listener listener;
    private OfflineTts tts;
    private AudioTrack track;
    private List<String> lines=List.of();
    private String word;
    private int index;
    private volatile int generation;
    private volatile boolean closed;
    private boolean ready,playing,whole,pendingPlay,failed;
    public boolean repeat;
    private float rate=1f;
    public SpeechPlayer(Context context,Listener listener) {
        this.listener=listener;
        audio=context.getSystemService(AudioManager.class);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes()).setOnAudioFocusChangeListener(change->{if(change<0)pause();}).build();
        Context application=context.getApplicationContext();
        synthesis.execute(()->{
            try {
                tts=OfflineVoice.create(application);
                main.post(()->{if(closed)return;ready=true;listener.changed("内置美国英语男声已就绪 · 离线可用",-1);if(pendingPlay)start();});
            } catch(Exception|LinkageError e) {
                main.post(()->{if(closed)return;failed=true;pendingPlay=false;listener.changed("内置语音加载失败，请重新打开应用",-1);});
            }
        });
    }
    private static AudioAttributes attributes(){return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();}
    public void load(List<String> sentences){stop();word=null;lines=sentences;index=0;}
    public void setRate(float value){rate=value;if(playing){boolean wasWhole=whole;pause();whole=wasWhole;start();}}
    public void full(){word=null;index=0;whole=true;start();}
    public void one(int sentence){word=null;index=sentence;whole=false;start();}
    public void word(String text){word=text;whole=false;start();}
    public void resume(){word=null;whole=true;start();}
    private void start(){
        stop();
        if(word==null&&lines.isEmpty()){listener.changed("请先选择一篇英文",-1);return;}
        if(!ready){pendingPlay=!failed;listener.changed(failed?"内置语音加载失败，请重新打开应用":"离线语音正在加载，完成后自动朗读",-1);return;}
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){listener.changed("暂时无法播放，请稍后重试",-1);return;}
        playing=true;speak();
    }
    private boolean active(int token){return !closed&&token==generation;}
    private void speak(){
        int token=generation,sentence=word==null?index:-1;String text=word==null?lines.get(index):word;float speed=rate;
        listener.changed(sentence<0?"正在准备单词发音":"正在准备离线朗读 · 第"+(sentence+1)+"句",sentence);
        synthesis.execute(()->{
            AudioTrack output=null;
            try {
                if(!active(token))return;
                // The native callback expects Kotlin's typed invoke(float[]), which a Java lambda does not expose.
                // Use the no-callback API; pause/selection changes discard completed stale synthesis below.
                GeneratedAudio generated=tts.generate(text.replace('’','\'').trim(),OfflineVoice.SPEAKER,speed);
                if(!active(token))return;
                float[] samples=generated.getSamples();
                if(samples.length==0)throw new IllegalStateException("语音为空");
                short[] pcm=new short[samples.length];for(int i=0;i<pcm.length;i++)pcm[i]=(short)(Math.max(-1f,Math.min(1f,samples[i]))*32767);
                int sampleRate=generated.getSampleRate();
                synchronized(trackLock){
                    if(!active(token))return;
                    int buffer=AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
                    if(buffer<=0)throw new IllegalStateException("音频设备不可用");
                    output=new AudioTrack.Builder().setAudioAttributes(attributes())
                            .setAudioFormat(new AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                            .setBufferSizeInBytes(Math.max(buffer,sampleRate)).setTransferMode(AudioTrack.MODE_STREAM).build();
                    track=output;output.play();
                }
                main.post(()->{if(active(token))listener.changed(sentence<0?"正在朗读单词":"正在朗读 · 第"+(sentence+1)+"句",sentence);});
                int offset=0;
                while(active(token)&&offset<pcm.length){
                    int written=output.write(pcm,offset,Math.min(4096,pcm.length-offset),AudioTrack.WRITE_NON_BLOCKING);
                    if(written<0)throw new IllegalStateException("音频输出失败");
                    offset+=written;if(written==0)Thread.sleep(8);
                }
                while(active(token)&&Integer.toUnsignedLong(output.getPlaybackHeadPosition())<pcm.length)Thread.sleep(8);
                main.post(()->{
                    if(!active(token)||!playing)return;
                    if(whole&&index+1<lines.size()){index++;speak();}
                    else if(whole&&repeat){index=0;speak();}
                    else{playing=false;index=0;audio.abandonAudioFocusRequest(focus);listener.changed("朗读完成",-1);}
                });
            } catch(Exception e){
                main.post(()->{if(active(token)){stop();listener.changed("离线朗读失败，请重试并检查媒体音量",-1);}});
            } finally {
                synchronized(trackLock){if(output!=null){if(track==output)track=null;output.release();}}
            }
        });
    }
    public void pause(){stop();listener.changed("已暂停 · 继续时从本句开头朗读",index);}
    public void stop(){
        generation++;playing=false;pendingPlay=false;
        synchronized(trackLock){if(track!=null){track.pause();track.flush();}}
        audio.abandonAudioFocusRequest(focus);
    }
    public void close(){closed=true;stop();synthesis.execute(()->{if(tts!=null)tts.release();});synthesis.shutdown();}
}
