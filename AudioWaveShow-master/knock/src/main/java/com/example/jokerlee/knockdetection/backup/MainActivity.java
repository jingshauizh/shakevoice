/** By Geir Turtum and Torgeir Lien
 * 
 */

package com.example.jokerlee.knockdetection.backup;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jokerlee.knockdetection.KnockDetector;
import com.example.jokerlee.knockdetection.R;


public class MainActivity extends AppCompatActivity {

	//The abstract class KnockDetector requires the implementation of void knockDetected(int) method
	private KnockDetector mKnockDetector;

    private TextView tips;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        tips = (TextView) findViewById(R.id.tip_text);

		mKnockDetector = new KnockDetector(this){
            @Override
            public void knockDetected(int knockCount) {
                switch (knockCount){
                    case 2:
                        Log.d("media","next song");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Detect knocked twice.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case 3:
                        Log.d("media","pause/play");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Detect knocked three times.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        pausePlay();
                        break;
                    default:
                        Toast.makeText(MainActivity.this,"default",Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
	}

	public void onBackPressed (){
		super.onBackPressed();
		mKnockDetector.stopDetecting();
	}

	public void onResume(){
		super.onResume();
		mKnockDetector.startDetecting();
	}

    @Override
    protected void onDestroy() {
        mKnockDetector.stopDetecting();
        super.onDestroy();
    }

    public void controlClick(View view){
        Button button = (Button) view;
        if( mKnockDetector.isDetecting() ) {
            mKnockDetector.stopDetecting();
            button.setText("Start Detecting");
            tips.setText("Pause knock event detecting.");
        } else {
            mKnockDetector.startDetecting();
            button.setText("Stop Detecting");
            tips.setText("Detecting knock event, now.");
        }

    }

	public void pausePlay(){
		// 暂停
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		Intent i = new Intent("com.android.music.musicservicecommand");
		if( audioManager.isMusicActive() ) {
			i.putExtra("command", "pause");
		} else {
			i.putExtra("command", "play");
		}
		sendBroadcast(i);
	}

}
