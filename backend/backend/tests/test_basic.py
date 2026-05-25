def test_home_endpoint(client):
    """Testuje czy główny endpoint / odpowiada statusem 200."""
    response = client.get("/")

    assert response.status_code == 200
    assert response.get_json() == {"message": "DriveOps API dziala"}