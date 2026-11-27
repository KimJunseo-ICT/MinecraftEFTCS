import org.objectweb.asm.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;

/** Forge 6.0.x wrapper names omit the enclosing class of a nested subscriber. */
public final class EventSubscriberNamesCheck {
    private static String simple(String name) { return name.substring(Math.max(name.lastIndexOf('/'),name.lastIndexOf('$'))+1); }
    public static void main(String[] args) throws Exception {
        Map<String,String> wrappers=new TreeMap<>();List<String> collisions=new ArrayList<>();int[] count={0};
        try(var jar=new JarFile(Path.of(args[0]).toFile())) {
            for(var entry:Collections.list(jar.entries())) {
                if(!entry.getName().startsWith("com/minecrafteft/") || !entry.getName().endsWith(".class")) continue;
                try(var input=jar.getInputStream(entry)) {
                    var reader=new ClassReader(input);String owner=reader.getClassName();
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override public AnnotationVisitor visitAnnotation(String annotation,boolean visible) {
                                    if(annotation.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")) {
                                        Type[] parameters=Type.getArgumentTypes(descriptor);
                                        if(parameters.length!=1) throw new AssertionError("Unexpected event signature: "+owner+"."+name);
                                        String key=owner.substring(0,owner.lastIndexOf('/')+1)+"__"+simple(owner)+"_"+name+"_"+simple(parameters[0].getInternalName());
                                        String handler=owner+"."+name+descriptor,previous=wrappers.putIfAbsent(key,handler);
                                        if(previous!=null && !previous.equals(handler)) collisions.add(key+"\n  "+previous+"\n  "+handler);
                                        count[0]++;
                                    }
                                    return null;
                                }
                            };
                        }
                    },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                }
            }
        }
        if(!collisions.isEmpty()) throw new AssertionError("Forge subscriber wrapper collisions:\n"+String.join("\n",collisions));
        System.out.println("EventSubscriberNamesCheck: "+count[0]+" subscriber methods have unique Forge wrapper names");
    }
}
