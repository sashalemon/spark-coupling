{% for record in structs %}
    case "{{ record.name }}" => new {{ record.name }}Reader()
{%- endfor %}
