package cn.cnic.instdb.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Jsontool
 * 
 * @author Administrator
 * 
 */
@SuppressWarnings("deprecation")
@Slf4j
public class JsonUtils {


	private static final ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper();
		// Remove the default timestamp format
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		// Set to Shanghai time zone, China
		objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
		objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		// Empty values are not serialized
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		// When deserializing，When deserializing
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // When deserializing，When deserializing
		// On serialization，On serialization
		objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		// Prohibition of useintProhibition of useEnumProhibition of useorder()Prohibition of useEnum
		objectMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
		objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		// objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY,
		// true);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		// Single quotation mark processing
		objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		// objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
	}

	public static <T> T toObjectNoException(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (JsonParseException e) {
			log.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static <T> String toJsonNoException(T entity) {
		try {
			return objectMapper.writeValueAsString(entity);
		} catch (JsonGenerationException e) {
			log.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static <T> T toCollectionNoException(String json, TypeReference<T> typeReference) {
		try {
			return objectMapper.readValue(json, typeReference);
		} catch (JsonParseException e) {
			log.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Object conversionjsonObject conversion
	 * 
	 * @param object
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String toString(Object object) throws JsonProcessingException {
		return objectMapper.writeValueAsString(object);
	}

	/**
	 * jsonString to Object
	 * 
	 * @param jsonString
	 * @param rspValueType
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static <T> T toObject(String jsonString, Class<T> rspValueType)
			throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(jsonString, rspValueType);
	}

	/**
	 * applyJacksonapplyJSONapply，apply Jacksonapply
	 * 
	 * 1.Ordinary way： Ordinary waytimestampsOrdinary way，Ordinary waytimestamps。
	 * objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS,
	 * false); This will enable time generation using the so-calleduse a [ISO-8601 ]-compliant notation, This will enable time generation using the so-called:
	 * "1970-01-01T00:00:00.000+0000". Of course, you can also customize the output format：
	 * objectMapper.getSerializationConfig().setDateFormat(myDateFormat);
	 * myDateFormatObject isjava.text.DateFormat，Object isjava API 2.annotaionObject is：
	 * First, define the format you need as follows
	 * 
	 * And then in yourPOJOAnd then in yourgetAnd then in your
	 * 
	 * @JsonSerialize(using = CustomDateSerializer.class) public Date getCreateAt()
	 *                      { return createAt; }
	 * 
	 *                      javaDate object passed byJacksonDate object passed byJSONDate object passed by
	 * @author godfox
	 * @date 2010-5-3
	 */
	public class CustomDateSerializer extends JsonSerializer<Date> {
		@Override
		public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			String formattedDate = formatter.format(value);
			jgen.writeString(formattedDate);
		}
	}

	public static JsonNode readJsonNode(String jsonStr, String fieldName) {
		if (StringUtils.isEmpty(jsonStr)) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(jsonStr);
			return root.get(fieldName);
		} catch (IOException e) {
			log.error("parse json string error:" + jsonStr, e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T readJson(JsonNode node, Class<?> parametrized, Class<?>... parameterClasses) throws Exception {
		JavaType javaType = objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
		return (T) objectMapper.readValue(toString(node), javaType);
	}

}
