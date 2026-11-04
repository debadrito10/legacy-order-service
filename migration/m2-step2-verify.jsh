import com.noduco.legacy.*;
System.out.println("R1 encode: " + new LegacyIdEncoder().encode("101"));
System.out.println("R4 gst: " + new TaxRules().gst(100.0));
System.out.println("R5 length: " + TextUtils.fastLength("Asha"));
System.out.println("R6 utf8: " + TextUtils.utf8Size("Zo\u00eb"));
/exit
