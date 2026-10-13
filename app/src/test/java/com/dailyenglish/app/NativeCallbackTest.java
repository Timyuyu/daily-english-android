package com.dailyenglish.app;
import org.junit.Test;
import kotlin.jvm.functions.Function1;
import static org.junit.Assert.*;

public class NativeCallbackTest {
    @Test public void javaLambdaCannotSupplyTheSignatureRequiredByNativeTtsCallback() {
        Function1<float[],Integer> oldCallback=samples->1;
        // offline-tts.cc requires invoke([F)Ljava/lang/Integer;, not the erased Java invoke(Object).
        assertThrows(NoSuchMethodException.class,()->oldCallback.getClass().getMethod("invoke",float[].class));
        assertEquals(1,oldCallback.invoke(new float[]{0.1f}).intValue());
    }
}
