package com.llmscannerseguridad.domain.scoring;

import java.math.BigDecimal;
import java.util.*;
import com.llmscannerseguridad.domain.evaluation.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class ScoringPolicyV1Test {
    private final ScoringPolicyV1 policy = new ScoringPolicyV1();
    private ScoringPolicyV1.Input input(Severity severity,String confidence) { return new ScoringPolicyV1.Input(severity,new BigDecimal(confidence)); }
    @ParameterizedTest @CsvSource({"0.00,0","0.09,0","0.10,1","0.29,1","0.30,2","0.49,2","0.50,3","0.60,3","0.69,3","0.70,4","0.89,4","0.90,5","0.95,5","1.00,5"})
    void decimalHalfUpBoundaries(String confidence,int bonus) {
        assertThat(policy.confidenceBonus(new BigDecimal(confidence))).isEqualTo(bonus);
        int[] bases={10,40,70,90};
        for(var severity:Severity.values()) assertThat(policy.calculate(List.of(input(severity,confidence)))).isEqualTo(bases[severity.ordinal()]+bonus);
    }
    @ParameterizedTest @CsvSource({"0,NO_FINDINGS","1,LOW","39,LOW","40,MEDIUM","69,MEDIUM","70,HIGH","89,HIGH","90,CRITICAL","100,CRITICAL"})
    void riskLevelIsSeparateAndHasExactBands(int value,RiskLevel level) { assertThat(RiskLevel.fromScore(value)).isEqualTo(level); }
    @Test void exactExamplesAndFloors() {
        assertThat(policy.calculate(List.of())).isZero();
        var low=input(Severity.LOW,"0.90");var medium=input(Severity.MEDIUM,"0.90");var high=input(Severity.HIGH,"0.90");var critical=input(Severity.CRITICAL,"0.90");
        assertThat(policy.calculate(List.of(input(Severity.LOW,"0.00")))).isEqualTo(10);
        assertThat(policy.calculate(List.of(input(Severity.HIGH,"0.00")))).isEqualTo(70);
        assertThat(policy.calculate(List.of(input(Severity.CRITICAL,"0.00")))).isEqualTo(90);
        assertThat(policy.calculate(Collections.nCopies(3,low))).isEqualTo(18);
        assertThat(policy.calculate(Collections.nCopies(4,low))).isEqualTo(19);
        assertThat(policy.calculate(Collections.nCopies(20,low))).isEqualTo(25);
        assertThat(policy.calculate(List.of(high,low))).isEqualTo(76);
        assertThat(policy.calculate(List.of(high,medium,low))).isEqualTo(81);
        assertThat(policy.calculate(Collections.nCopies(4,high))).isEqualTo(85);
        assertThat(policy.calculate(List.of(critical,high))).isEqualTo(100);
        assertThat(policy.calculate(List.of(low,low))).isEqualTo(16);
        assertThat(policy.calculate(List.of(input(Severity.HIGH,"0"),input(Severity.LOW,"0.09")))).isEqualTo(71);
    }
    @ParameterizedTest @CsvSource({"75,0,75","75,10,76","75,99,84","75,100,85","75,101,85","95,45,99","95,50,100","95,60,100"})
    void remainderUsesFloorAndHasBoundedIncrement(int maximum,int remainder,int score) {
        assertThat(policy.aggregate(maximum,remainder)).isEqualTo(score);
    }
    @Test void permutationMonotonicityAndCaps() {
        var random=new Random(10);
        for(var highest:Severity.values()) {
            var values=new ArrayList<ScoringPolicyV1.Input>();
            for(int count=0;count<30;count++) {
                int before=policy.calculate(values);
                values.add(input(highest,"0.90"));
                int value=policy.calculate(values);
                assertThat(value).isGreaterThanOrEqualTo(before).isLessThanOrEqualTo(new int[]{25,55,85,100}[highest.ordinal()]);
                Collections.shuffle(values,random);assertThat(policy.calculate(values)).isEqualTo(value);
            }
        }
        for(var severity:Severity.values()) {
            int previous=0;
            for(int i=0;i<=100;i++) {
                int value=policy.calculate(List.of(new ScoringPolicyV1.Input(severity,BigDecimal.valueOf(i,2)),input(Severity.LOW,"0.90")));
                assertThat(value).isGreaterThanOrEqualTo(previous);previous=value;
            }
        }
        int previous=0;
        for(var severity:Severity.values()) {
            int value=policy.calculate(List.of(input(severity,"0.90"),input(Severity.LOW,"0.90")));
            assertThat(value).isGreaterThanOrEqualTo(previous);previous=value;
        }
    }
    @Test void invalidInputsAndOverflowAreRejected() {
        for(String invalid:List.of("-0.01","1.01")) {
            assertThatThrownBy(()->input(Severity.LOW,invalid)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(()->policy.confidenceBonus(new BigDecimal(invalid))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(()->new ScoringPolicyV1.Input(null,BigDecimal.ZERO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(()->new ScoringPolicyV1.Input(Severity.LOW,null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(()->RiskLevel.fromScore(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->RiskLevel.fromScore(101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->policy.calculate(Collections.nCopies(Integer.MAX_VALUE/95+1,input(Severity.CRITICAL,"1"))))
                .isInstanceOf(ArithmeticException.class);
    }
}
