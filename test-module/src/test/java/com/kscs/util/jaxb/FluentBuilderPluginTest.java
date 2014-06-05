package com.kscs.util.jaxb;

import com.kscs.jaxb2.contract.test.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the FluentBuilderPlugin
 */
public class FluentBuilderPluginTest {
	@Test
	public void testFluentBuilderSimple() {
		final DerivedType derivedType = DerivedType.builder().withChildren("a", "b", "c", "d", "e").withName("MyName").withSimpleElement("Simple Name").build();

		Assert.assertNotNull(derivedType);
		Assert.assertNotNull(derivedType.getName());
		Assert.assertNotNull(derivedType.getChildren());
		Assert.assertNotNull(derivedType.getSimpleElement());
		Assert.assertEquals("a", derivedType.getChildren().get(0));
		Assert.assertEquals(5, derivedType.getChildren().size());
	}

	@Test
	public void testFluentBuilderComplex() {
		final Tourist tourist = Tourist.builder().withAddress().withCity().withPostalCode("AAAA").withTown("Bonn").end().withStreet("Hermannstädter Str. 10").end().withDestination("Thailand").build();

		Assert.assertEquals("AAAA", tourist.getAddress().getCity().getPostalCode());
		Assert.assertEquals("Bonn", tourist.getAddress().getCity().getTown());
		Assert.assertEquals("Hermannstädter Str. 10", tourist.getAddress().getStreet());
		Assert.assertEquals("Thailand", tourist.getDestination());
		CompanyMember.builder().withUniqueName("Lala").build();
	}

	@Test
	public void testFluentBuilderNull() {
		final Tourist tourist = Tourist.builder().withAddress().withCity(null).withStreet("Hermannstädter Str. 10").end().withDestination("Thailand").build();

		Assert.assertNull(tourist.getAddress().getCity());
		Assert.assertEquals("Hermannstädter Str. 10", tourist.getAddress().getStreet());
		Assert.assertEquals("Thailand", tourist.getDestination());

		final Tourist tourist2 = Tourist.builder().withAddress().withCity().withInhabitants(null,null,null).end().withStreet("Hermannstädter Str. 10").end().withDestination(null).build();

		Assert.assertNotNull(tourist2.getAddress().getCity());
		Assert.assertEquals(3, tourist2.getAddress().getCity().getInhabitants().size());
		Assert.assertNull(tourist2.getAddress().getCity().getInhabitants().get(0));
		Assert.assertNull(tourist2.getAddress().getCity().getInhabitants().get(1));
		Assert.assertNull(tourist2.getAddress().getCity().getInhabitants().get(2));
		Assert.assertEquals("Hermannstädter Str. 10", tourist2.getAddress().getStreet());
		Assert.assertNull(tourist2.getDestination());

	}

	@Test
	public void testFluentBuilderCopy() {
		final Tourist tourist = Tourist.builder().withAddress().withCity().withInhabitants(Worker.builder().withName("aa").withAddress().withStreet("wwww").end().withCompany("company").build()).withPostalCode("AAAA").withTown("Bonn").end().withStreet("Hermannstädter Str. 10").end().withDestination("Thailand").build();

		Assert.assertEquals("AAAA", tourist.getAddress().getCity().getPostalCode());
		Assert.assertEquals("Bonn", tourist.getAddress().getCity().getTown());
		Assert.assertEquals("Hermannstädter Str. 10", tourist.getAddress().getStreet());
		Assert.assertEquals("Thailand", tourist.getDestination());
		Assert.assertEquals(1, tourist.getAddress().getCity().getInhabitants().size());
		Assert.assertEquals("wwww", tourist.getAddress().getCity().getInhabitants().get(0).getAddress().getStreet());
		CompanyMember.builder().withUniqueName("Lala").build();

		final Tourist tourist2 = Tourist.copyOf(tourist).build();
		Assert.assertEquals("AAAA", tourist2.getAddress().getCity().getPostalCode());
		Assert.assertEquals("Bonn", tourist2.getAddress().getCity().getTown());
		Assert.assertEquals("Hermannstädter Str. 10", tourist2.getAddress().getStreet());
		Assert.assertEquals("Thailand", tourist2.getDestination());
		Assert.assertEquals(1, tourist2.getAddress().getCity().getInhabitants().size());
		Assert.assertEquals("wwww", tourist2.getAddress().getCity().getInhabitants().get(0).getAddress().getStreet());

		final Tourist tourist3 = Tourist.copyOf(tourist).withAddress().withCity().withTown("Köln").withPostalCode("53000").withInhabitants(tourist.getAddress().getCity().getInhabitants().get(0)).end().end().build();
		Assert.assertEquals("53000", tourist3.getAddress().getCity().getPostalCode());
		Assert.assertEquals("Köln", tourist3.getAddress().getCity().getTown());
		Assert.assertEquals("Thailand", tourist3.getDestination());
		Assert.assertEquals(1, tourist3.getAddress().getCity().getInhabitants().size());
		Assert.assertEquals("wwww", tourist3.getAddress().getCity().getInhabitants().get(0).getAddress().getStreet());

	}

	@Test
	public void testPropertyPath() {
		final PropertyTree propertyPath = Tourist.Select._root().address().city().inhabitants().address().street().build();

		Assert.assertNotNull(propertyPath.get("address"));
		Assert.assertNotNull(propertyPath.get("address").get("city"));
		Assert.assertNull(propertyPath.get("address").get("city").get("town"));
		Assert.assertNull(propertyPath.get("address").get("city").get("postalCode"));
		Assert.assertNotNull(propertyPath.get("address").get("city").get("inhabitants"));
		Assert.assertNull(propertyPath.get("address").get("city").get("inhabitants").get("name"));
		Assert.assertNull(propertyPath.get("address").get("city").get("inhabitants").get("phoneNumber"));
		Assert.assertNotNull(propertyPath.get("address").get("city").get("inhabitants").get("address"));
		Assert.assertNotNull(propertyPath.get("address").get("city").get("inhabitants").get("address").get("street"));
	}

	@Test
	public void testPropertyPathComplex() {
		final PropertyTree propertyPath = Tourist.Select._root().address().city().inhabitants().address().street()._parent.city()._root.destination().build();

		Assert.assertNotNull(propertyPath.get("address"));
		Assert.assertNotNull(propertyPath.get("address").get("city"));
		Assert.assertNull(propertyPath.get("address").get("city").get("town"));
		Assert.assertNull(propertyPath.get("address").get("city").get("postalCode"));
		Assert.assertNotNull(propertyPath.get("address").get("city").get("inhabitants"));
		Assert.assertNull(propertyPath.get("address").get("city").get("inhabitants").get("name"));
		Assert.assertNull(propertyPath.get("address").get("city").get("inhabitants").get("phoneNumber"));
		Assert.assertNotNull(propertyPath.get("address").get("city").get("inhabitants").get("address"));
		Assert.assertNotNull(propertyPath.get("address").get("city").get("inhabitants").get("address").get("street"));
		Assert.assertNotNull(propertyPath.get("address").get("city").get("inhabitants").get("address").get("city"));
		Assert.assertNull(propertyPath.get("address").get("city").get("inhabitants").get("address").get("createdAt"));
		Assert.assertNotNull(propertyPath.get("destination"));
		Assert.assertNull(propertyPath.get("departureDate"));
	}

	@Test
	public void testTransformerPath() {
		final TransformerPath propertyPath = Tourist.Transform._root(new PropertyTransformer<Void,Tourist>(){

			@Override
			public Tourist transform(final PropertyInfo<Void, Tourist> propertyInfo, final Void sourceInstance, final Tourist sourcePropertyValue) {
				return null;
			}
		}).address(new PropertyTransformer<Tourist,Address>() {

			@Override
			public Address transform(final PropertyInfo<Tourist, Address> propertyInfo, final Tourist sourceInstance, final Address sourcePropertyValue) {
				return null;
			}
		}).build();
	}

	@Test
	public void testBuilderInterface() {
		final IdentifyingProperties.BuildSupport<Void> buildSupport = CompanyMember.builder();

		final IdentifyingProperties idp = buildSupport.withDisplayName("dpn1").withId("ID").withUniqueName("un").build();

		Assert.assertEquals("dpn1", idp.getDisplayName());
		Assert.assertEquals("ID", idp.getId());
		Assert.assertEquals("un", idp.getUniqueName());
	}

//	@Test
//	public void testTransformerPathComplex() {
//		final TransformerPath propertyPath = Tourist.Select._root(true).address(true).city(false).inhabitants(false).address(true).street(false)._parent.city(false)._root.destination(false).build();
//
//		Assert.assertTrue(propertyPath.get("address").includes());
//		Assert.assertTrue(propertyPath.get("address").get("city").includes());
//		Assert.assertFalse(propertyPath.get("address").get("city").get("town").includes());
//		Assert.assertFalse(propertyPath.get("address").get("city").get("postalCode").includes());
//		Assert.assertTrue(propertyPath.get("address").get("city").get("inhabitants").includes());
//		Assert.assertFalse(propertyPath.get("address").get("city").get("inhabitants").get("name").includes());
//		Assert.assertFalse(propertyPath.get("address").get("city").get("inhabitants").get("phoneNumber").includes());
//		Assert.assertTrue(propertyPath.get("address").get("city").get("inhabitants").get("address").includes());
//		Assert.assertFalse(propertyPath.get("address").get("city").get("inhabitants").get("address").get("street").includes());
//		Assert.assertFalse(propertyPath.get("address").get("city").get("inhabitants").get("address").get("city").includes());
//		Assert.assertTrue(propertyPath.get("address").get("city").get("inhabitants").get("address").get("createdAt").includes());
//		Assert.assertFalse(propertyPath.get("destination").includes());
//		Assert.assertTrue(propertyPath.get("departureDate").includes());
//	}

}
