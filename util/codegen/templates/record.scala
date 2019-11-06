  class {{ record.name }} extends Struct {
    {%- for field in record.fields %}
    var {{ field.name }} = new {{ field.javolution_type }}()
    {%- endfor %}
    override def byteOrder(): ByteOrder = {
      ByteOrder.nativeOrder()
    }
  }
