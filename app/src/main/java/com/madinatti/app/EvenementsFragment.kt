package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EvenementsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_evenements, container, false
        )
    }

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Replace with real API data in Phase 2
        val events = listOf(
            EventItem(
                emoji = "🎭",
                title = "Festival Gnaoua",
                date = "15 Mars 2026",
                location = "Médina, Marrakech",
                imageEmoji = "🎶",
                interestedCount = 234
            ),
            EventItem(
                emoji = "🏃",
                title = "Marathon Marrakech",
                date = "22 Mars 2026",
                location = "Guéliz, Marrakech",
                imageEmoji = "🏅",
                interestedCount = 89
            ),
            EventItem(
                emoji = "🎨",
                title = "Expo Art Contemporain",
                date = "28 Mars 2026",
                location = "Hivernage, Marrakech",
                imageEmoji = "🖼️",
                interestedCount = 156
            ),
            EventItem(
                emoji = "🎵",
                title = "Concert Nass El Ghiwane",
                date = "5 Avril 2026",
                location = "Bab Jdid, Marrakech",
                imageEmoji = "🎤",
                interestedCount = 412
            ),
            EventItem(
                emoji = "🍽️",
                title = "Festival Gastronomique",
                date = "12 Avril 2026",
                location = "Palmeraie, Marrakech",
                imageEmoji = "👨‍🍳",
                interestedCount = 67
            ),
            EventItem(
                emoji = "📚",
                title = "Salon du Livre",
                date = "20 Avril 2026",
                location = "FIFM, Marrakech",
                imageEmoji = "📖",
                interestedCount = 198
            )
        )

        val tvCount = view.findViewById<TextView>(
            R.id.tvEventCount
        )
        tvCount?.text = "${events.size} à venir"

        val rv = view.findViewById<RecyclerView>(R.id.rvEvents)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = EventCardAdapter(
            events = events,
            onEventClick = { event ->
                EventDetailBottomSheet.newInstance(event)
                    .show(parentFragmentManager, "EventDetail")
            },
            onInterestedClick = { pos, event ->
                // TODO: Save to backend
            }
        )
    }


    val events = listOf(
        EventItem(
            emoji = "🎭",
            title = "Festival Gnaoua et Musiques du Monde",
            date = "15 - 18 Mars 2026",
            duration = "4 jours",
            venue = "Place Jemaa el-Fna",
            location = "Médina, Marrakech",
            startTime = "18:00",
            isFree = true,
            imageEmoji = "🎶",
            interestedCount = 234,
            description = "Le Festival Gnaoua et Musiques du Monde est l'un des plus grands festivals " +
                    "de musique en Afrique. Chaque année, il réunit des artistes gnaoua et des " +
                    "musiciens internationaux pour des nuits inoubliables de fusion musicale à Marrakech.",
            organizer = "Ville de Marrakech",
            organizerEmoji = "🏛️"
        ),
        EventItem(
            emoji = "🏃",
            title = "Marathon Marrakech",
            date = "22 Mars 2026",
            duration = "1 jour",
            venue = "Avenue Mohammed VI",
            location = "Guéliz, Marrakech",
            startTime = "07:00",
            isFree = false,
            price = "150 MAD",
            imageEmoji = "🏅",
            interestedCount = 89,
            description = "Le Marathon International de Marrakech traverse les plus beaux quartiers " +
                    "de la ville. Parcours de 42km, semi-marathon et course de 10km disponibles.",
            organizer = "Royal Athletics Club",
            organizerEmoji = "🏆"
        ),
        EventItem(
            emoji = "🎨",
            title = "Expo Art Contemporain",
            date = "28 Mars 2026",
            duration = "2 semaines",
            venue = "Musée Mohammed VI",
            location = "Hivernage, Marrakech",
            startTime = "10:00",
            isFree = true,
            imageEmoji = "🖼️",
            interestedCount = 156,
            description = "Exposition d'art contemporain mettant en lumière les artistes émergents " +
                    "du Maroc et d'Afrique. Peintures, sculptures et installations interactives.",
            organizer = "Fondation Culturelle",
            organizerEmoji = "🎨"
        ),
        EventItem(
            emoji = "🎵",
            title = "Concert Nass El Ghiwane",
            date = "5 Avril 2026",
            duration = "1 soir",
            venue = "Bab Jdid",
            location = "Médina, Marrakech",
            startTime = "20:00",
            isFree = false,
            price = "200 MAD",
            imageEmoji = "🎤",
            interestedCount = 412,
            description = "Concert exceptionnel du groupe légendaire Nass El Ghiwane. " +
                    "Une soirée inoubliable de musique marocaine authentique.",
            organizer = "Nass El Ghiwane Productions",
            organizerEmoji = "🎵"
        ),
        EventItem(
            emoji = "🍽️",
            title = "Festival Gastronomique",
            date = "12 Avril 2026",
            duration = "3 jours",
            venue = "La Palmeraie",
            location = "Palmeraie, Marrakech",
            startTime = "12:00",
            isFree = false,
            price = "50 MAD",
            imageEmoji = "👨‍🍳",
            interestedCount = 67,
            description = "Découvrez les saveurs du Maroc avec des chefs renommés. " +
                    "Dégustations, ateliers de cuisine et marché de producteurs locaux.",
            organizer = "Chefs Maroc",
            organizerEmoji = "👨‍🍳"
        ),
        EventItem(
            emoji = "📚",
            title = "Salon du Livre",
            date = "20 Avril 2026",
            duration = "5 jours",
            venue = "FIFM - Palais des Congrès",
            location = "Hivernage, Marrakech",
            startTime = "09:00",
            isFree = true,
            imageEmoji = "📖",
            interestedCount = 198,
            description = "Le Salon International du Livre de Marrakech accueille auteurs, " +
                    "éditeurs et lecteurs pour des rencontres, dédicaces et conférences.",
            organizer = "Ministère de la Culture",
            organizerEmoji = "📚"
        )
    )
}