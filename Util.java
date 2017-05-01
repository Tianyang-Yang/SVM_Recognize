package inc.yty.afinal.util;


public class Util {

    /**
     * 特征lable
     */
    private static final int FUN_1_MINIMUM_LABLE = 1;
    private static final int FUN_2_MAXIMUM_LABLE = 2;
    private static final int FUN_3_MEANCROSSINGSRATE_LABLE = 3;
    private static final int FUN_4_STANDARDDEVIATION_LABLE = 4;
    private static final int FUN_5_SPP_LABLE = 5;
    private static final int FUN_6_ENERGY_LABLE = 6;
    private static final int FUN_7_ENTROPY_LABLE = 7;
    private static final int FUN_8_CENTROID_LABLE = 8;
    private static final int FUN_9_MEAN_LABLE = 9;
    private static final int FUN_10_RMS_LABLE = 10;
    private static final int FUN_11_SMA_LABLE = 11;
    private static final int FUN_12_IQR_LABLE = 12;
    private static final int FUN_13_MAD_LABLE = 13;
    private static final int FUN_14_TENERGY_LABLE = 14;
    private static final int FUN_15_FDEV_LABLE = 15;
    private static final int FUN_16_FMEAN_LABLE = 16;
    private static final int FUN_17_SKEW_LABLE = 17;
    private static final int FUN_18_KURT_LABLE = 18;
    private static final int FUN_19_MEDIAN_LABLE = 19;

    /**
     * 把double数组转换成特征字符串数组
     *
     * @param doubleArr 数据
     * @param sinter    采样间隔(毫秒数)
     * @return
     */
    public static String[] dataToFeatures(double[] doubleArr, int sinter) {
        String[] featuresArr = new String[19];
        double[] fft = Features.fft(doubleArr.clone());
        featuresArr[0] = FUN_1_MINIMUM_LABLE + ":" + Features.minimum(doubleArr.clone());
        featuresArr[1] = FUN_2_MAXIMUM_LABLE + ":" + Features.maximum(doubleArr.clone());
        featuresArr[2] = FUN_3_MEANCROSSINGSRATE_LABLE + ":" + Features.meanCrossingsRate(doubleArr.clone());
        featuresArr[3] = FUN_4_STANDARDDEVIATION_LABLE + ":" + Features.standardDeviation(doubleArr.clone());
        featuresArr[4] = FUN_5_SPP_LABLE + ":" + Features.spp(fft.clone());
        featuresArr[5] = FUN_6_ENERGY_LABLE + ":" + Features.energy(fft.clone());
        featuresArr[6] = FUN_7_ENTROPY_LABLE + ":" + Features.entropy(fft.clone());
        featuresArr[7] = FUN_8_CENTROID_LABLE + ":" + Features.centroid(fft.clone());
        featuresArr[8] = FUN_9_MEAN_LABLE + ":" + Features.mean(doubleArr.clone());
        featuresArr[9] = FUN_10_RMS_LABLE + ":" + Features.rms(doubleArr.clone());
        featuresArr[10] = FUN_11_SMA_LABLE + ":" + Features.sma(doubleArr.clone(), sinter / 1000.0);
        featuresArr[11] = FUN_12_IQR_LABLE + ":" + Features.iqr(doubleArr.clone());
        featuresArr[12] = FUN_13_MAD_LABLE + ":" + Features.mad(doubleArr.clone());
        featuresArr[13] = FUN_14_TENERGY_LABLE + ":" + Features.tenergy(doubleArr.clone());
        featuresArr[14] = FUN_15_FDEV_LABLE + ":" + Features.fdev(fft.clone());
        featuresArr[15] = FUN_16_FMEAN_LABLE + ":" + Features.fmean(fft.clone());
        featuresArr[16] = FUN_17_SKEW_LABLE + ":" + Features.skew(fft.clone());
        featuresArr[17] = FUN_18_KURT_LABLE + ":" + Features.kurt(fft.clone());
        featuresArr[18] = FUN_19_MEDIAN_LABLE + ":" + Features.median(doubleArr.clone());
        return featuresArr;
    }

}
