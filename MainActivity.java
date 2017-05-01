package inc.yty.afinal;

import android.Manifest;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import inc.yty.afinal.svmlib.svm_predict;
import inc.yty.afinal.svmlib.svm_scale;
import inc.yty.afinal.svmlib.svm_train;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import static inc.yty.afinal.util.Util.dataToFeatures;

public class MainActivity extends AppCompatActivity {

    private Spinner spAction, spPostion;
    private int lableAction = 1, labelPostion = 1, lable = 101;

    private SensorManager sensorManager;
    private int sinter = 1000 * 1000 / 32;
    private CollectioinLisener collectioinLisener;
    private UnderstandLietner understandLietner;

    private String directory;       // 项目文件目录

    private String trainFilePath;       // 样本文件路径
    private String scaleFilePath;       // 归一化后的文件路径
    private String rangeFilePath;       // 归一化规则文件
    private String modelFilePath;       // model文件路径
    private String modelTrainInfo;      // 训练model的时候控制台的信息
    private String predictFilePath;     // 使用规划化后的文件测试model的结果文件
    private String predictAccuracyFilePath;   // model精度文件

    private RandomAccessFile trainFile;

    private TextView tvCollectionNum, tvCollectionAcc, tvAccuracy, tvAction, tvPostioin;

    private svm_model svmModel;
    private String[] actioins;
    private String[] postioins;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        actioins = getResources().getStringArray(R.array.Actions);
        postioins = getResources().getStringArray(R.array.Positions);

        tvCollectionNum = (TextView) findViewById(R.id.tv_collection_num);
        tvCollectionAcc = (TextView) findViewById(R.id.tv_collction_acc);
        tvAccuracy = (TextView) findViewById(R.id.tv_accuracy);

        tvAction = (TextView) findViewById(R.id.tv_action);
        tvPostioin = (TextView) findViewById(R.id.tv_postion);

        // 动态请求权限 6.0以上设备
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

        // 创建项目目录
        directory = Environment.getExternalStorageDirectory() + File.separator + "YTYdata";
        File file = new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }

        // 给文件路径赋值
        trainFilePath = directory + File.separator + "train.txt";
        scaleFilePath = directory + File.separator + "scale.txt";
        rangeFilePath = directory + File.separator + "range.txt";
        modelFilePath = directory + File.separator + "model.txt";
        modelTrainInfo = directory + File.separator + "modelTrainInfo.txt";
        predictFilePath = directory + File.separator + "predict.txt";
        predictAccuracyFilePath = directory + File.separator + "predictAccuracy.txt";

        try {
            trainFile = new RandomAccessFile(trainFilePath, "rwd");
        } catch (FileNotFoundException e) {
            Log.e("FFFFFFFFFFFFFFF", trainFilePath);
            e.printStackTrace();
        }

        collectioinLisener = new CollectioinLisener();
        understandLietner = new UnderstandLietner();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        spAction = (Spinner) findViewById(R.id.sp_action);
        spPostion = (Spinner) findViewById(R.id.sp_postion);

        spAction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                lableAction = position + 1;
                lable = lableAction * 100 + labelPostion;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spPostion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                labelPostion = position + 1;
                lable = lableAction * 100 + labelPostion;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 点击开始采集的时候调用这个方法
     *
     * @param view
     */
    public void startCollection(View view) {
        sensorManager.registerListener(collectioinLisener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sinter);
    }

    /**
     * 点击停止采集的时候调用这个方法
     *
     * @param view
     */
    public void stopCollection(View view) {
        sensorManager.unregisterListener(collectioinLisener);
    }

    /**
     * 点击训练model的时候调用这个方法
     *
     * @param view
     */
    public void trainModel(View view) {
        new MyTrainTask().execute();
    }

    /**
     * 点击开始识别的时候调用这个方法
     *
     * @param view
     */
    public void startUnderstand(View view) {
        // 1. 加载model
        // 2. 读入range文件 对加速的值做规划
        // 3. smv_predict
        try {
            svmModel = svm.svm_load_model(new BufferedReader(new InputStreamReader(new FileInputStream(modelFilePath))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        readRange();

        sensorManager.registerListener(understandLietner, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sinter);
    }

    List<String> range = new ArrayList<>();
    double scaleLower, scaleUpper;      // 规划的最大值和最小值
    double[][] featureRange;        // 规划区间
    int featureCount;

    /**
     * 读取range文件
     */
    private void readRange() {
        range.clear();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(rangeFilePath)));
            String string = null;
            while ((string = bufferedReader.readLine()) != null) {
                range.add(string);
            }
            featureCount = range.size() - 2;
            featureRange = new double[featureCount][2];

            // 读取lower 和 upper
            String lowerAndUpper = range.get(1);
            String[] split = lowerAndUpper.split(" ");  // 以空格切分字符串
            scaleLower = Double.parseDouble(split[0]);
            scaleUpper = Double.parseDouble(split[1]);

            // 读取每一行的特征值
            for (int i = 0; i < featureCount; i++) {
                String[] featureLowerAndUpper = range.get(i + 2).split(" ");
                featureRange[i][0] = Double.parseDouble(featureLowerAndUpper[1]);
                featureRange[i][1] = Double.parseDouble(featureLowerAndUpper[2]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 点击停止识别的时候调用这个方法
     *
     * @param view
     */
    public void stopUnderstand(View view) {
        sensorManager.unregisterListener(understandLietner);
    }


    private int collectionNnm; // 当前采集了几个样本

    /**
     * 采集时候调用
     */
    class CollectioinLisener implements SensorEventListener {

        int num = 128;
        int currentNum;
        double[] data = new double[num];

        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double acc = Math.sqrt(x * x + y * y + z * z);
            tvCollectionAcc.setText(String.valueOf(acc));
            if (currentNum >= num) {
                // 写入到文件里边
                String[] features = dataToFeatures(data, sinter);
                writeFile(features);
                currentNum = 0;
                collectionNnm++;
                tvCollectionNum.setText(String.valueOf(collectionNnm));
            }
            data[currentNum++] = acc;
        }

        private void writeFile(String[] features) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(lable);
            for (String feature : features) {
                stringBuilder.append(" " + feature);
            }
            stringBuilder.append("\n");
            try {
                byte[] data=stringBuilder.toString().getBytes();
                trainFile.write(data);
            } catch (Exception e) {
                Log.e("AAAAAAAAAAAAAAAAA", "SSSSSSSSSSSSSSSSSSSSSS");
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    class UnderstandLietner implements SensorEventListener {

        int num = 128;
        int currentNum;
        double[] data = new double[num];

        @Override
        public void onSensorChanged(SensorEvent event) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double acc = Math.sqrt(x * x + y * y + z * z);
            tvCollectionAcc.setText(String.valueOf(acc));
            if (currentNum >= num) {
                // 写入到文件里边
                String[] features = dataToFeatures(data, sinter);

                double code = underStand(features);

                System.out.println("-------------:" + code);

                int act = (int) (code / 100);
                int pos = (int) (code - act * 100);

                tvAction.setText(actioins[act - 1]);
                tvPostioin.setText(postioins[pos - 1]);
                // writeFile(features);
                currentNum = 0;
            }
            data[currentNum++] = acc;
        }

        String[] featureString;

        private double underStand(String[] features) {
            svm_node[] svm_nodes = new svm_node[featureCount];
            svm_node svm_node;
            for (int i = 0; i < features.length; i++) {
                featureString = features[i].split(":");
                svm_node = new svm_node();
                svm_node.index = Integer.parseInt(featureString[0]);
                svm_node.value = Double.parseDouble(featureString[1]);
                svm_nodes[i] = svm_node;
            }
            return svm.svm_predict(svmModel, svm_nodes);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }


    /**
     * 训练model的类
     */
    class MyTrainTask extends AsyncTask<Void, Void, Void> {

        /**
         * 开始执行
         */
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            tarinModel(trainFilePath,
                    rangeFilePath,
                    scaleFilePath,
                    modelFilePath,
                    predictFilePath,
                    modelTrainInfo,
                    predictAccuracyFilePath);
            return null;
        }

        /**
         * 执行结束
         *
         * @param aVoid
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(predictAccuracyFilePath)));
                String readLine = reader.readLine();
                System.out.println(readLine);
                tvAccuracy.setText(readLine);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 训练模型
     *
     * @param trainFile
     * @param rangeFile
     * @param scaleFile
     * @param modelFile
     * @param predictResult
     * @param modelTrainInfo
     * @param prdictAccuracy
     */
    public void tarinModel(String trainFile, String rangeFile, String scaleFile, String modelFile, String predictResult, String modelTrainInfo, String prdictAccuracy) {
        creatScaleFile(new String[]{"-l", "0", "-u", "1", "-s", rangeFile, trainFile}, scaleFile);
        creatModelFile(new String[]{"-s", "0", "-c", "128.0", "-t", "2", "-g", "8.0", "-e", "0.1", scaleFile, modelFile}, modelTrainInfo);
        creatPredictFile(new String[]{scaleFile, modelFile, predictResult}, prdictAccuracy);
        //svm_train.main(new String[]{"-s", "0", "-c", "128.0", "-t", "2", "-g", "8.0", "-e", "0.1", scaleFile, modelFile});
        //svm_predict.main(new String[]{scaleFile, modelFile, predictResult});
    }


    /**
     * 训练数据train 进行归一化处理并生生scale文件
     *
     * @param args      String[] args = new String[]{"-l","0","-u","1",path+"/train"};
     * @param scalePath 结果输出文件路径
     */
    private static void creatScaleFile(String[] args, String scalePath) {
        FileOutputStream fileOutputStream = null;
        PrintStream printStream = null;
        try {
            File file = new File(scalePath);
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            printStream = new PrintStream(fileOutputStream);
            // old stream
            PrintStream oldStream = System.out;
            System.setOut(printStream);//重新定义system.out
            svm_scale.main(args);//开始归一化
            System.setOut(oldStream);//回复syste.out
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (printStream != null) {
                    printStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void creatModelFile(String[] args, String outInfo) {
        FileOutputStream fileOutputStream = null;
        PrintStream printStream = null;
        try {
            File file = new File(outInfo);
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            printStream = new PrintStream(fileOutputStream);
            // old stream
            PrintStream oldStream = System.out;
            System.setOut(printStream);//重新定义system.out
            svm_train.main(args);//开始训练模型
            System.setOut(oldStream);//回复syste.out
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (printStream != null) {
                    printStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void creatPredictFile(String[] args, String outInfo) {
        FileOutputStream fileOutputStream = null;
        PrintStream printStream = null;
        try {
            File file = new File(outInfo);
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            printStream = new PrintStream(fileOutputStream);
            // old stream
            PrintStream oldStream = System.out;
            System.setOut(printStream);//重新定义system.out
            svm_predict.main(args);//开始测试精度
            System.setOut(oldStream);//回复syste.out
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (printStream != null) {
                    printStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
