package com.zqy.tinker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity {

    Unbinder unbinder;

    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.button1)
    Button button1;
    @BindView(R.id.button2)
    Button button2;
    @BindView(R.id.button3)
    Button button3;
    @BindView(R.id.button4)
    Button button4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        text.setText("我是基准包");

        // 第一次补丁
        text.setText("基准包是：app-1.0.0-0725-09-36-29");
        button1.setText("appVersion = 1.0.0");

        // 第二次补丁
        button2.setText("appVersion = 1.0.1");
    }

    @OnClick({R.id.button1, R.id.button2, R.id.button3, R.id.button4})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button1: {
                Toast.makeText(this, "button1是个补丁！", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.button2: {
                Toast.makeText(this, "button2是个补丁！", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.button3: {

                break;
            }
            case R.id.button4: {

                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }


}
