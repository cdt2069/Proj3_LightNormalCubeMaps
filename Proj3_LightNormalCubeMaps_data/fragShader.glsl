#version 430

in vec2 tc;
out vec4 color;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform vec3 ambientColor;
uniform vec3 objectColor;
layout (binding=0) uniform sampler2D s;
uniform sampler2D textureSampler;

uniform vec3 lightPosition;
uniform vec3 lightColor;
const float shininess = 32.0;

void main(void)
{	
	//color = texture(s,tc);
	
	// texture color
	vec4 textureColor = texture(textureSampler, tc);
	
	// Ambient Light
	vec3 ambient = ambientColor * objectColor.rgb;
	
	// Diffuse Light
	vec3 lightDir = normalize(lightPosition - vec3(mv_matrix * vec4(0.0, 0.0, 0.0, 1.0)));
	float diff = max(dot(normalize(textureColor.rgb), lightDir), 0.0);
	vec3 diffuse = diff * lightColor * objectColor.rgb;
	
	//Specular Light
	vec3 viewDir = normalize(vec3(0.0, 0.0, 1.0) - vec3(mv_matrix * vec4(0.0, 0.0, 0.0, 1.0)));
	vec3 reflectDir = reflect(-lightDir, normalize(textureColor.rgb));
	float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
	vec3 specular = spec * lightColor;
	
	vec3 ADSColor = (ambient + diffuse + specular) * textureColor.rgb;
	
	color = vec4(ADSColor, textureColor.a);
	
}
