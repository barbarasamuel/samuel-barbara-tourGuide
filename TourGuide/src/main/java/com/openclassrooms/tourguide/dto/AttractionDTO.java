package com.openclassrooms.tourguide.dto;

import lombok.Data;

@Data
public class AttractionDTO {
    // Name of Tourist attraction,
    String touristAttractionName;
    // Tourist attractions lat/long,
    Double attractionLatitude;
    Double attractionLongitude;
    // The user's location lat/long,
    Double userLocationLatitude;
    Double userLocationLongitude;
    // The distance in miles between the user's location and each of the attractions.
    Double distanceInMiles;
    // The reward points for visiting each Attraction.
    //    Note: Attraction reward points can be gathered from RewardsCentral
    Long rewardPoints;
}
