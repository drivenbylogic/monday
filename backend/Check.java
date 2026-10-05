import java.lang.reflect.Method;
public class Check {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.openai.models.ChatCompletionMessageParam");
        for (Method m : clazz.getMethods()) {
            if (m.getName().startsWith("of")) {
                System.out.println(m.getName());
            }
        }
    }
}
