package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

public class ParentSpecimenUniqueIdLabelToken extends AbstractSpecimenLabelToken {

	@Autowired
	private DaoFactory daoFactory;
	
	public ParentSpecimenUniqueIdLabelToken() {
		this.name = "PSPEC_UID";
	}
	
	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public String getLabelN(Specimen specimen, String... args) {
		String format = "%d";
		if (args != null && args.length > 0) {
			int digit = Integer.parseInt(args[0]);
			format = digit == 0 ? format : "%0" + digit + "d";
		}

		if (specimen.getParentSpecimen() == null) {
			return "";
		}
		
		String pidStr = specimen.getParentSpecimen().getId().toString();
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(name, pidStr);
		return String.format(format, uniqueId);
	}

	public String getLabel(Specimen specimen) {
		return getLabelN(specimen);
	}
	
	@Override
	public int validate(Object object, String input, int startIdx, String ... args) {
		String regx = "\\d+";
		if (args != null && args.length > 0) {
			int digit = Integer.parseInt(args[0]);
			regx = digit == 0 ? regx : "\\d{" + digit + "}";
		}

		Pattern pattern = Pattern.compile(regx);
		Matcher matcher = pattern.matcher(input.substring(startIdx));
		if (matcher.find() && matcher.start() == 0) {
			return startIdx + matcher.group(0).length();
		}

		return startIdx;
	}
}
