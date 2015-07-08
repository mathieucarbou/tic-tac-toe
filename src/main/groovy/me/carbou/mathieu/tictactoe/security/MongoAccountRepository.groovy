package me.carbou.mathieu.tictactoe.security

import com.guestful.backend.common.*
import com.guestful.backend.model.PublisherStatus
import com.guestful.jaxrs.security.realm.Account
import com.guestful.jaxrs.security.realm.AccountRepository
import com.guestful.jaxrs.security.subject.SubjectContext
import com.guestful.jaxrs.security.token.AuthenticationToken
import com.guestful.jaxrs.security.token.FacebookToken
import com.guestful.jaxrs.security.token.LoginPasswordToken
import com.guestful.jaxrs.security.token.PassthroughToken
import me.carbou.mathieu.tictactoe.db.DB

import javax.inject.Inject
import java.security.Principal

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@javax.inject.Singleton
class MongoAccountRepository implements AccountRepository {

    @Inject DB guestful

    @Override
    Account findAccount(AuthenticationToken token) {
        if (token.token) {
            switch (token.system) {

                case 'gamer':
                    switch (token) {

                        case PassthroughToken:
                            Map guest = guestful.guests.findOne([email: token.token.toString().trim().toLowerCase()], [id: 1])
                            return toGuestAccount(guest)
                            break;

                        case FacebookToken:
                            Map guest = guestHelper.findGuestByFacebook((token as FacebookToken).me, APIToken.current, SubjectContext.getSubject(token.system).getRequest().getLocale())
                            return toGuestAccount(guest)
                            break

                        default:
                            throw new UnsupportedOperationException(token.toString())
                    }
                    break

                default:
                    throw new UnsupportedOperationException(token.toString())
            }
        } else {
            return null
        }
    }

    @Override
    Account findAccount(String system, Principal principal) {
        switch (system) {

            case 'guest':
                Map guest = guestful.guests.findOne([id: principal.name], [id: 1])
                return toGuestAccount(guest)
                break

            case 'user':
                Map user = guestful.users.findOne([
                    id: principal.name,
                    status: UserStatus.ACTIVE,
                    passwordHash: [$exists: true, $ne: null]
                ], [
                    _id: 0,
                    id: 1,
                    passwordHash: 1,
                    roles: 1,
                    status: 1
                ])
                return toUserAccount(user)
                break

            default:
                throw new UnsupportedOperationException(system + ":" + principal)
        }
    }

    private Account toGuestAccount(Map guest) {
        if (guest == null) return null
        Account account = new Account(guest.id as String)
        account.locked = false
        return account
    }

    private Account toUserAccount(Map user) {
        if (user == null || !user.passwordHash) return null

        Account account = new Account(user.id as String, user.passwordHash as String)
        account.locked = user.status as UserStatus != UserStatus.ACTIVE

        // add user roles
        Set<String> accesses = new TreeSet<>()
        user.roles.each { String role, Map data ->
            account.addRole(role)
            accesses.addAll(data.accesses ?: [])
        }
        if (!accesses) {
            accesses << '*'
        } else if (Role.STAFF in account.roles || Role.MANAGER in account.roles) {
            // if user has some accesses set AND it is a staff or manager, add dashboard access also
            accesses << APIToken.ACCOUNT_SYSTEM
        }
        accesses.each { account.addRole("api:account:${it}") }

        // add permissions if user has publisher role
        if (Role.PUBLISHER in account.roles) {
            guestful.publishers.find(
                [
                    id: [$in: user.roles[Role.PUBLISHER].publishers ?: []],
                    status: [$in: PublisherStatus.UNDELETED]
                ],
                [_id: 0, id: 1]
            ).each { account.addPermission("publishers:${it.id}") }
        }

        // add permissions if user has editor role
        if (Role.EDITOR in account.roles || Role.SUPER_EDITOR in account.roles) {
            guestful.categories.find(
                [
                    id: Role.SUPER_EDITOR in account.roles ? null : [$in: user.roles[Role.EDITOR].categories ?: []],
                    deleted: false
                ].removeNulls(),
                [_id: 0, id: 1]
            ).each { account.addPermission("manage:categories:${it.id}") }
        }

        // add permissions if user is manager or staff
        if (Role.STAFF in account.roles || Role.MANAGER in account.roles) {

            // collect all restaurants ids to fetch once and add per-restaurant permissions
            Set<String> restaurantIds = new HashSet<>()
            restaurantIds.addAll(user.roles[Role.STAFF]?.restaurants ?: [])
            restaurantIds.addAll(user.roles[Role.MANAGER]?.restaurants ?: [])

            if (restaurantIds) {
                List<Map> restaurants = guestful.restaurants.find(
                    [
                        id: [$in: restaurantIds],
                        deleted: false
                    ],
                    [_id: 0, id: 1]
                )
                if (restaurants) {

                    // add roles for staff if any
                    restaurants
                        .findAll { ((it.id in (user.roles[Role.STAFF]?.restaurants ?: [])) || (it.id in (user.roles[Role.MANAGER]?.restaurants ?: []))) }
                        .each { account.addPermission("restaurants:${it.id}") }

                    // add roles for manager if any
                    restaurants
                        .findAll { it.id in (user.roles[Role.MANAGER]?.restaurants ?: []) }
                        .each { account.addPermission("manage:restaurants:${it.id}") }
                }
            }

        }

        return account
    }

}
