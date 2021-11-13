import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ClassTransformer  implements ClassFileTransformer {
    private int count=0;

    @Override
    public byte[] transform(final ClassLoader loader,
                            final String className,
                            final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain,
                            final byte[] classfileBuffer) {

        byte[] byteCode = classfileBuffer;

        System.out.println("[Agent]: "+String.format("Class N %s loaded. ", ++count)+ "Class name:"+className.replaceAll("/", ".") );

        if ("TransactionProcessor".equals(className.replaceAll("/", "."))) {
            try {

                ClassPool pool = ClassPool.getDefault();
                CtClass ctClass = pool.get("TransactionProcessor");
                CtField ctField1 = new CtField(CtClass.longType, "minValue", ctClass);
                ctField1.setModifiers(Modifier.PUBLIC);
                ctClass.addField(ctField1, "0l");
                CtField ctField2 = new CtField(CtClass.longType, "maxValue", ctClass);
                ctField2.setModifiers(Modifier.PUBLIC);
                ctClass.addField(ctField2, "0l");
                CtField ctField3 = new CtField(CtClass.longType, "allValue", ctClass);
                ctField3.setModifiers(Modifier.PUBLIC);
                ctClass.addField(ctField3, "0l");



                ctClass.addMethod(CtNewMethod.make("public void to_time(long inTime){\n" +
                        "         if(inTime>maxValue){\n" +
                        "                maxValue=inTime;\n" +
                        "         }\n" +
                        "         if(inTime<minValue||minValue==0){\n" +
                        "             minValue=inTime;\n" +
                        "         }\n" +
                        "          allValue+=inTime;\n" +
                        "    }", ctClass));

                ctClass.addMethod(CtNewMethod.make("public void print_time(){\n" +
                        " System.out.println(\"Max time is \"+maxValue);\n" +
                        "          System.out.println(\"Min time is \"+ minValue);\n" +
                        "          allValue=allValue/10;\n" +
                        "          System.out.println(\"Average time is \"+allValue);" +
                        "}", ctClass));

                CtMethod method = ctClass.getDeclaredMethod("processTransaction");
                method.addLocalVariable("startMs", CtClass.longType);
                method.insertBefore("startMs = System.currentTimeMillis();\n" +
                        "        txNum+=99;");
                method.insertAfter ("to_time(System.currentTimeMillis() - startMs);" +
                        "if(txNum==108){" +
                        "print_time();" +
                        "}");





                try {
                    byteCode = ctClass.toBytecode();
                    ctClass.detach();
                    return byteCode;
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }

        }


        return byteCode;
    }
}
